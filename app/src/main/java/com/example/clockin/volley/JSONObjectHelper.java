package com.example.clockin.volley;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;

public class JSONObjectHelper {
    private JSONObject jsonObject;

    public JSONObjectHelper(Map<String, String> mapBody) {
        jsonObject = new JSONObject(mapBody);
        if (mapBody.containsKey("landmark")) {
            String str = mapBody.get("landmark");
            assert str != null;
            String[] string = str.replaceAll("\\[", "")
                    .replaceAll("]", "")
                    .replaceAll(" ", "")
                    .split(",");

            // declaring an array with the size of string
            int[] arr = new int[string.length];

            // parsing the String argument as a signed decimal
            // integer object and storing that integer into the
            // array
            for (int i = 0; i < string.length; i++) {
                arr[i] = Integer.parseInt(string[i]);
            }
            try {
                jsonObject.put("landmark", new JSONArray(arr));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        if (mapBody.containsKey("manager")) {
            boolean manager = Boolean.parseBoolean(mapBody.get("manager"));
            try {
                jsonObject.put("manager", manager);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (mapBody.containsKey("wage")) {
            int wage = Integer.parseInt(mapBody.get("wage"));
            try {
                jsonObject.put("wage", wage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }


}
