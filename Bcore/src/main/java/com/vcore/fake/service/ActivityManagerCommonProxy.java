package com.vcore.fake.service;

import static android.content.pm.PackageManager.GET_META_DATA;

import static com.vcore.fake.service.IActivityManagerProxy.getIntOrLongValue;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import com.vcore.BlackBoxCore;
import com.vcore.app.BActivityThread;
import com.vcore.core.env.AppSystemEnv;
import com.vcore.entity.AppConfig;
import com.vcore.fake.delegate.ServiceConnectionDelegate;
import com.vcore.fake.frameworks.BActivityManager;
import com.vcore.fake.hook.MethodHook;
import com.vcore.fake.hook.ProxyMethod;
import com.vcore.fake.provider.FileProviderHandler;
import com.vcore.utils.ArrayUtils;
import com.vcore.utils.ComponentUtils;
import com.vcore.utils.MethodParameterUtils;
import com.vcore.utils.Slog;
import com.vcore.utils.compat.BuildCompat;
import com.vcore.fake.service.IActivityManagerProxy.GetIntentSender;
import com.vcore.fake.service.IActivityManagerProxy.BroadcastIntent;

public class ActivityManagerCommonProxy {
    public static final String TAG = "ActivityManagerCommonProxy";

    @ProxyMethod("startActivity")
    public static class StartActivity extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            Intent intent = getIntent(args);

            Slog.d(TAG, "Hook in : " + intent);
            assert intent != null;
            if (intent.getParcelableExtra("_B_|_target_") != null) {
                return method.invoke(who, args);
            }

            if (ComponentUtils.isRequestInstall(intent)) {
                File file = FileProviderHandler.convertFile(BActivityThread.getApplication(), intent.getData());
                if (BlackBoxCore.get().requestInstallPackage(file, BActivityThread.getUserId())) {
                    return 0;
                }

                intent.setData(FileProviderHandler.convertFileUri(BActivityThread.getApplication(), intent.getData()));
                return method.invoke(who, args);
            }

            String dataString = intent.getDataString();
            if (dataString != null && dataString.equals("package:" + BActivityThread.getAppPackageName())) {
                intent.setData(Uri.parse("package:" + BlackBoxCore.getHostPkg()));
            }

            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveActivity(intent, GET_META_DATA, getResolvedType(args),
                    BActivityThread.getUserId());
            if (resolveInfo == null) {
                String origPackage = intent.getPackage();
                if (intent.getPackage() == null && intent.getComponent() == null) {
                    intent.setPackage(BActivityThread.getAppPackageName());
                } else {
                    origPackage = intent.getPackage();
                }

                resolveInfo = BlackBoxCore.getBPackageManager().resolveActivity(intent, GET_META_DATA, getResolvedType(args),
                        BActivityThread.getUserId());
                if (resolveInfo == null) {
                    intent.setPackage(origPackage);
                    return method.invoke(who, args);
                }
            }

            intent.setExtrasClassLoader(who.getClass().getClassLoader());
            intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            BlackBoxCore.getBActivityManager().startActivityAms(BActivityThread.getUserId(), getIntent(args),
                    getResolvedType(args), getResultTo(args), getResultWho(args),
                    getRequestCode(args), getFlags(args), getOptions(args));
            return 0;
        }

        private Intent getIntent(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 3;
            } else {
                index = 2;
            }

            if (args[index] instanceof Intent) {
                return (Intent) args[index];
            }

            for (Object arg : args) {
                if (arg instanceof Intent) {
                    return (Intent) arg;
                }
            }
            return null;
        }

        private String getResolvedType(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 4;
            } else {
                index = 3;
            }

            if (args[index] instanceof String) {
                return (String) args[index];
            }

            for (Object arg : args) {
                if (arg instanceof String) {
                    return (String) arg;
                }
            }
            return null;
        }

        private IBinder getResultTo(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 5;
            } else {
                index = 4;
            }

            if (args[index] instanceof IBinder) {
                return (IBinder) args[index];
            }

            for (Object arg : args) {
                if (arg instanceof IBinder) {
                    return (IBinder) arg;
                }
            }
            return null;
        }

        private String getResultWho(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 6;
            } else {
                index = 5;
            }

            if (args[index] instanceof String) {
                return (String) args[index];
            }

            for (Object arg : args) {
                if (arg instanceof String) {
                    return (String) arg;
                }
            }
            return null;
        }

        private int getRequestCode(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 7;
            } else {
                index = 6;
            }

            if (args[index] instanceof Integer) {
                return (Integer) args[index];
            }

            for (Object arg : args) {
                if (arg instanceof Integer) {
                    return (Integer) arg;
                }
            }
            return 0;
        }

        private int getFlags(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 8;
            } else {
                index = 7;
            }

            if (args[index] instanceof Integer) {
                return (Integer) args[index];
            }

            for (Object arg : args) {
                if (arg instanceof Integer) {
                    return (Integer) arg;
                }
            }
            return 0;
        }

        private Bundle getOptions(Object[] args) {
            int index;
            if (BuildCompat.isR()) {
                index = 9;
            } else {
                index = 8;
            }

            if (args[index] instanceof Bundle) {
                return (Bundle) args[index];
            }

            for (Object arg : args) {
                if (arg instanceof Bundle) {
                    return (Bundle) arg;
                }
            }
            return null;
        }
    }

    @ProxyMethod("startActivities")
    public static class StartActivities extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int index = getIntents();
            Intent[] intents = (Intent[]) args[index++];
            String[] resolvedTypes = (String[]) args[index++];
            IBinder resultTo = (IBinder) args[index++];
            Bundle options = (Bundle) args[index];

            if (!ComponentUtils.isSelf(intents)) {
                return method.invoke(who, args);
            }

            for (Intent intent : intents) {
                intent.setExtrasClassLoader(who.getClass().getClassLoader());
            }
            return BlackBoxCore.getBActivityManager().startActivities(BActivityThread.getUserId(), intents, resolvedTypes, resultTo, options);
        }

        public int getIntents() {
            return 2;
        }
    }

    @ProxyMethod("activityResumed")
    public static class ActivityResumed extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            BlackBoxCore.getBActivityManager().onActivityResumed((IBinder) args[0]);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("activityDestroyed")
    public static class ActivityDestroyed extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            BlackBoxCore.getBActivityManager().onActivityDestroyed((IBinder) args[0]);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("finishActivity")
    public static class FinishActivity extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            BlackBoxCore.getBActivityManager().onFinishActivity((IBinder) args[0]);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAppTasks")
    public static class GetAppTasks extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getCallingPackage")
    public static class GetCallingPackage extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return BlackBoxCore.getBActivityManager().getCallingPackage((IBinder) args[0], BActivityThread.getUserId());
        }
    }

    @ProxyMethod("getCallingActivity")
    public static class GetCallingActivity extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return BlackBoxCore.getBActivityManager().getCallingActivity((IBinder) args[0], BActivityThread.getUserId());
        }
    }

    @ProxyMethod("getIntentSenderWithFeature")
    public static class GetIntentSenderWithFeature extends IActivityManagerProxy.GetIntentSender {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("broadcastIntentWithFeature")
    public static class BroadcastIntentWithFeature extends IActivityManagerProxy.BroadcastIntent {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Intent intent = new Intent((Intent) args[2]);
            String type = (String) args[3];
            intent.setDataAndType(intent.getData(), type);
            Intent newIntent = BlackBoxCore.getBActivityManager().sendBroadcast(intent, type, BActivityThread.getUserId());
            if (newIntent != null) {
                args[1] = newIntent;
            } else {
                return 0;
            }
            if (args[7] instanceof String || args[7] instanceof String[]) {
                // clear the permission
                args[7] = null;
            }
            int index = ArrayUtils.indexOfFirst(args, Boolean.class);
            args[index] = false;
            MethodParameterUtils.replaceLastUserId(args);
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return 0; //ActivityManager.BROADCAST_SUCCESS
//                return -1; //ActivityManager.BROADCAST_STICKY_CANT_HAVE_PERMISSION
//                return -2; //ActivityManager.BROADCAST_FAILED_USER_STOPPED
            }
        }
    }

    @ProxyMethod("bindService")
    public static class BindService extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IInterface iInterface = (IInterface) args[0];
            IBinder iBinder = (IBinder) args[1];
            Intent intent = (Intent) args[2];
            String resolvedType = (String) args[3];
            IServiceConnection connection = (IServiceConnection) args[4];
            //待修复
            //- StartService = connection;
            //-----
            ComponentName component = intent.getComponent();
            long flags = getIntOrLongValue(args[5]);
            int userId = intent.getIntExtra("_B_|_UserId", -1);
            userId = userId == -1 ? BActivityThread.getUserId() : userId;
            ResolveInfo resolveInfo = BlackBoxCore.getBPackageManager().resolveService(intent, 0, resolvedType, userId);
            if (component != null && component.getPackageName().equals(BlackBoxCore.getHostPkg())) {
                return method.invoke(who, args);
            }
            int callingPkgIdx = isIsolated() ? 7 : (char) 6;
            if (args.length >= 8 && (args[callingPkgIdx] instanceof String)) {
                args[callingPkgIdx] = BlackBoxCore.getHostPkg();
            }
            if (resolveInfo == null) {
                if (component == null || !AppSystemEnv.isOpenPackage(component.getPackageName())) {
                    Log.e("ActivityManager", "Block bindService: " + intent);
                    return 0;
                }
                MethodParameterUtils.replaceLastUserId(args);
                return method.invoke(who, args);
            }
            if ((flags & (-2147483648L)) != 0) {
                if (BuildCompat.isU()) {
                    args[5] = Long.valueOf(flags & 2147483647L);
                } else {
                    args[5] = Integer.valueOf((int) (flags & 2147483647L));
                }
            }
            AppConfig appConfig = BActivityManager.get().initProcess(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name,userId);
            if (appConfig == null) {
                Log.e("ActivityManager", "failed to initProcess for bindService: " + component);
                return 0;
            }
            Intent proxyIntent = BlackBoxCore.getBActivityManager().bindService(intent,
                    connection == null ? null : connection.asBinder(),
                    resolvedType,
                    userId);
            args[2] = proxyIntent;
            args[4] = ServiceConnectionDelegate.createProxy(connection, intent);
//            WeakReference<?> weakReference = BRLoadedApkServiceDispatcherInnerConnection.get(connection).mDispatcher();
//            if (weakReference != null) {
//                BRLoadedApkServiceDispatcher.get(weakReference.get())._set_mConnection(ServiceConnectionDelegate.createProxy(connection, intent));
//            }
            return method.invoke(who, args);
        }

        @Override
        protected boolean isEnable() {
            return BlackBoxCore.get().isBlackProcess() || BlackBoxCore.get().isServerProcess();
        }

        protected boolean isIsolated() {
            return false;
        }
    }
}
