package com.nativecamera;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

class PromiseResolve {

    String fileUri, name, type, data;

    public PromiseResolve(String fileUri, String name, String type) {
        this.fileUri = fileUri;
        this.name = name;
        this.type = type;
    }

    public PromiseResolve(String data) {
        this.data = data;
    }

    public WritableMap getObject() {
        WritableMap map = Arguments.createMap();
        if (data != null) {
            map.putString("data", data);
            return map;
        }
        map.putString("fileUri", fileUri);
        map.putString("name", name);
        map.putString("type", type);
        return map;
    }
}
