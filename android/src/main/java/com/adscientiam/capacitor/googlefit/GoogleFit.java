package com.adscientiam.capacitor.googlefit;

import android.Manifest;
import android.content.Intent;

import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@CapacitorPlugin()
public class GoogleFit extends Plugin {

    public static final String TAG = "HistoryApi";

    private GoogleSignInAccount getAccount() {
        return GoogleSignIn.getLastSignedInAccount(getActivity());
    }

    public boolean hasFitnessPermissions() {
        GoogleSignInAccount account = getAccount();
        if (account == null) {
            return false;
        }
        Set<Scope> grantedScopes = account.getGrantedScopes();
        int count = 0;
        for(Scope scope: grantedScopes) {
            if (scope.getScopeUri().equals(Scopes.FITNESS_ACTIVITY_READ)) {
                count += 1;
            } else if (scope.getScopeUri().equals(Scopes.FITNESS_LOCATION_READ)) {
                count += 1;
            }
        }
        return count >= 2;
    }

    @PluginMethod()
    public void connectToGoogleFit(PluginCall call) {
        String clientID = call.getString("clientID");
        GoogleSignInAccount account = getAccount();
//        if (account == null || !this.hasFitnessPermissions() || account.getServerAuthCode() == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope(Scopes.FITNESS_ACTIVITY_READ), new Scope(Scopes.FITNESS_LOCATION_READ))
                    .requestServerAuthCode(clientID, true)
                    .requestEmail()
                    .build();
            GoogleSignInClient signInClient = GoogleSignIn.getClient(this.getActivity(), gso);
            Intent intent = signInClient.getSignInIntent();
            startActivityForResult(call, intent, "signInResult");
//        } else {
//            this.signInResult(call, null);
//        }
    }

    @PluginMethod()
    public void isAllowed(PluginCall call) {
        final JSObject result = new JSObject();
        if (this.hasFitnessPermissions()) {
            result.put("allowed", true);
        } else {
            result.put("allowed", false);
        }
        call.resolve(result);
    }

    @ActivityCallback
    private void signInResult(PluginCall call, ActivityResult result) {
        final JSObject resultObject = new JSObject();

        try {
            GoogleSignInAccount account = getAccount();
            String authCode = account.getServerAuthCode();
            if (this.hasFitnessPermissions() && authCode != null) {
                resultObject.put("authCode", authCode);
            } else {
                resultObject.put("authCode", null);
            }
        } catch (Exception e) {
            resultObject.put("authCode", null);
        }
        call.resolve(resultObject);
    }

    @PluginMethod()
    public Task<DataReadResponse> getHistory(final PluginCall call) throws ParseException {
        GoogleSignInAccount account = getAccount();

        if (account == null) {
            call.reject("No access");
            return null;
        }

        long startTime = dateToTimestamp(call.getString("startTime"));
        long endTime = dateToTimestamp(call.getString("endTime"));
        if (startTime == -1 || endTime == -1) {
            call.reject("Must provide a start time and end time");
            return null;
        }

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_SPEED)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .enableServerQueries()
                .build();

        return Fitness.getHistoryClient(getActivity(), account).readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        List<Bucket> buckets = dataReadResponse.getBuckets();
                        JSONArray days = new JSONArray();
                        for (Bucket bucket : buckets) {
                            JSONObject summary = new JSONObject();
                            try {
                                summary.put("start", timestampToDate(bucket.getStartTime(TimeUnit.MILLISECONDS)));
                                summary.put("end", timestampToDate(bucket.getEndTime(TimeUnit.MILLISECONDS)));
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    if (dataSet.getDataPoints().size() > 0) {
                                        switch (dataSet.getDataType().getName()) {
                                            case "com.google.distance.delta":
                                                summary.put("distance", dataSet.getDataPoints().get(0).getValue(Field.FIELD_DISTANCE));
                                                break;
                                            case "com.google.speed.summary":
                                                summary.put("speed", dataSet.getDataPoints().get(0).getValue(Field.FIELD_AVERAGE));
                                                break;
                                            case "com.google.calories.expended":
                                                summary.put("calories", dataSet.getDataPoints().get(0).getValue(Field.FIELD_CALORIES));
                                                break;
                                            default:
                                                Log.i(TAG, "need to handle " + dataSet.getDataType().getName());
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                call.reject(e.getMessage());
                                return;
                            }
                            days.put(summary);
                        }
                        JSObject result = new JSObject();
                        result.put("days", days);
                        call.resolve(result);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        call.reject(e.getMessage());
                    }
                });
    }

    @PluginMethod()
    public Task<DataReadResponse> getHistoryActivity(final PluginCall call) throws ParseException {
        final GoogleSignInAccount account = getAccount();

        if (account == null) {
            call.reject("No access");
            return null;
        }

        long startTime = dateToTimestamp(call.getString("startTime"));
        long endTime = dateToTimestamp(call.getString("endTime"));
        if (startTime == -1 || endTime == -1) {
            call.reject("Must provide a start time and end time");
            return null;
        }

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_SPEED)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .bucketByActivitySegment(1, TimeUnit.MINUTES)
                .enableServerQueries()
                .build();

        return Fitness.getHistoryClient(getActivity(), account).readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        List<Bucket> buckets = dataReadResponse.getBuckets();
                        JSONArray activities = new JSONArray();
                        for (Bucket bucket : buckets) {
                            JSONObject summary = new JSONObject();
                            try {
                                summary.put("start", timestampToDate(bucket.getStartTime(TimeUnit.MILLISECONDS)));
                                summary.put("end", timestampToDate(bucket.getEndTime(TimeUnit.MILLISECONDS)));
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    if (dataSet.getDataPoints().size() > 0) {
                                        switch (dataSet.getDataType().getName()) {
                                            case "com.google.distance.delta":
                                                summary.put("distance", dataSet.getDataPoints().get(0).getValue(Field.FIELD_DISTANCE));
                                                break;
                                            case "com.google.speed.summary":
                                                summary.put("speed", dataSet.getDataPoints().get(0).getValue(Field.FIELD_AVERAGE));
                                                break;
                                            case "com.google.calories.expended":
                                                summary.put("calories", dataSet.getDataPoints().get(0).getValue(Field.FIELD_CALORIES));
                                                break;
                                            default:
                                                Log.i(TAG, "need to handle " + dataSet.getDataType().getName());
                                        }
                                    }
                                }
                                summary.put("activity", bucket.getActivity());
                            } catch (JSONException e) {
                                call.reject(e.getMessage());
                                return;
                            }
                            activities.put(summary);
                        }
                        JSObject result = new JSObject();
                        result.put("activities", activities);
                        call.resolve(result);
                    }
                });
    }

    private String timestampToDate(long timestamp) {
        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return df.format(cal.getTime());
    }

    private long dateToTimestamp(String date) {
        if (date.isEmpty()) {
            return -1;
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            return f.parse(date).getTime();
        } catch (ParseException e) {
            return -1;
        }
    }
}
