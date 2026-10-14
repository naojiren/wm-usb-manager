package com.wm.usbmanager;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import org.json.JSONObject;
import java.io.OutputStream;

public final class BridgeClient {
    public static JSONObject call(JSONObject request) throws Exception { return call(request, false); }
    public static JSONObject call(JSONObject request, boolean validation) throws Exception {
        return call(request, validation, 180000);
    }
    public static JSONObject call(JSONObject request, boolean validation, int timeout) throws Exception {
        LocalSocket socket = new LocalSocket();
        try {
            socket.connect(new LocalSocketAddress(Policy.SOCKET + (validation ? ".validation" : ""), LocalSocketAddress.Namespace.ABSTRACT));
            socket.setSoTimeout(timeout);
            OutputStream out = socket.getOutputStream();
            out.write((request.toString() + "\n").getBytes("UTF-8")); out.flush();
            return new JSONObject(RootBridge.readLine(socket.getInputStream(), 4194304));
        } finally { socket.close(); }
    }
    // Used by the read-only validation runner under app_process, never exported as an Activity command.
    public static void main(String[] args) throws Exception {
        System.out.println(call(new JSONObject(args[0]), args.length > 1 && args[1].equals("--validation")));
    }
}
