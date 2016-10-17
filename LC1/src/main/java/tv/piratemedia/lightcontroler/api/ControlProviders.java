package tv.piratemedia.lightcontroler.api;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tv.piratemedia.lightcontroler.controller;

/**
 * Created by Eliot Stocker on 14/10/2016.
 */
public class ControlProviders {
    private static final String PROVIDER_CAT = "tv.piratemedia.lightcontroler.provider";
    private static final String PROVIDER_SELECT_ACTION = "tv.piratemedia.lightcontroler.provider.Select";

    public static final String ZONE_TYPE_COLOR   = "color";
    public static final String ZONE_TYPE_WHITE   = "white";
    public static final String ZONE_TYPE_UNKNOWN = "unknown";

    public static Provider[] listProviders(Context context) {
        final PackageManager pm = context.getPackageManager();
        Intent intent = new Intent();
        intent.addCategory(PROVIDER_CAT);
        intent.setAction(PROVIDER_SELECT_ACTION);
        List<ResolveInfo> infos = pm.queryBroadcastReceivers(intent, PackageManager.GET_RESOLVED_FILTER|PackageManager.GET_META_DATA);

        List<Provider> list = new ArrayList<>();
        int i = 0;
        for (ResolveInfo info : infos) {
            Provider p = new Provider(info.loadLabel(pm).toString(), info.activityInfo.packageName, info.activityInfo.name.substring(info.activityInfo.packageName.length()));
            int XMLResource = info.activityInfo.metaData.getInt(PROVIDER_CAT);
            try {
                XmlResourceParser xpp = pm.getResourcesForApplication(info.activityInfo.applicationInfo).getXml(XMLResource);
                xpp.next();
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("control-provider")) {
                        p.ColorBrightnessStatefull  = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "ColorBrightnessStatefull", false);
                        p.WhiteBrightnessStatefull  = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "WhiteBrightnessStatefull", false);
                        p.ColorHasTemperature       = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "ColorHasTemperature", false);
                        p.ColorTemperatureStatefull = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "ColorTemperatureStatefull", false);
                        p.WhiteTemperatureStatefull = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "WhiteTemperatureStatefull", false);
                        list.add(p);
                    }
                    eventType = xpp.next();
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            i++;
        }
        return list.toArray(new Provider[1]);
    }

    public static Provider getProvider(String pkg, Context context) {
        final PackageManager pm = context.getPackageManager();
        Intent intent = new Intent();
        intent.setPackage(pkg);
        intent.addCategory(PROVIDER_CAT);
        intent.setAction(PROVIDER_SELECT_ACTION);
        List<ResolveInfo> infos = pm.queryBroadcastReceivers(intent, PackageManager.GET_RESOLVED_FILTER|PackageManager.GET_META_DATA);

        ResolveInfo info = infos.get(0);

        Provider ret = new Provider(info.loadLabel(pm).toString(), info.activityInfo.packageName, info.activityInfo.name.substring(info.activityInfo.packageName.length()));
        int XMLResource = info.activityInfo.metaData.getInt(PROVIDER_CAT);
        try {
            XmlResourceParser xpp = pm.getResourcesForApplication(info.activityInfo.applicationInfo).getXml(XMLResource);
            xpp.next();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("control-provider")) {
                    ret.ColorBrightnessStatefull  = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "ColorBrightnessStatefull", false);
                    ret.WhiteBrightnessStatefull  = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "WhiteBrightnessStatefull", false);
                    ret.ColorHasTemperature       = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "ColorHasTemperature", false);
                    ret.ColorTemperatureStatefull = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "ColorTemperatureStatefull", false);
                    ret.WhiteTemperatureStatefull = xpp.getAttributeBooleanValue("http://schemas.android.com/apk/res-auto", "WhiteTemperatureStatefull", false);
                }
                eventType = xpp.next();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Provider getCurrentProvider(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String providerPkg = prefs.getString("pref_selected_provider", "");

        if(!providerPkg.equals("")) {
            return getProvider(providerPkg, context);
        }
        //no provider use built in
        return null;
    }

    public static void sendCommand(Provider provider, String action, Context context) {
        sendCommand(provider, action, null, -1, context);
    }

    public static void sendCommand(Provider provider, String action, String Type, int Zone, Context context) {
        final PackageManager pm = context.getPackageManager();
        Signature[] signs = null;
        try {
            signs = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        int sig = -1;
        if(signs != null) {
            for (Signature signature : signs) {
                sig = signature.hashCode();
            }
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setPackage(provider.Package);
        intent.addCategory(PROVIDER_CAT);
        intent.setAction(PROVIDER_CAT+"."+action);

        Intent pii = new Intent(context, controller.class);
        Random generator = new Random();
        PendingIntent ver = PendingIntent.getActivity(context, generator.nextInt(), pii, PendingIntent.FLAG_UPDATE_CURRENT);

        intent.putExtra("app_ver",  ver);
        intent.putExtra("app_sig",  sig);
        if(Type != null) {
            intent.putExtra("type", Type);
        }
        if(Zone > -1) {
            intent.putExtra("zone", Zone);
        }

        Log.d("SendIntent", "app_id: "+context.getPackageName()+" app_sig: "+sig);
        Log.d("SendIntent", "action: "+PROVIDER_CAT+"."+action+" Category: "+PROVIDER_CAT+" Package: "+provider.Package);
        context.sendBroadcast(intent);
    }
}
