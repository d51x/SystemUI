package com.android.systemui;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.IWindowManager.Stub;
import com.android.systemui.media.RingtonePlayer;
import com.android.systemui.power.PowerUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SystemUIService extends Service {
    final Object[] SERVICES;
    SystemUI[] mServices;

    public SystemUIService() {
        this.SERVICES = new Object[]{Integer.valueOf(0), PowerUI.class, RingtonePlayer.class};
    }

    private Class chooseClass(Object o) {
        if (o instanceof Integer) {
            try {
                return getClassLoader().loadClass(getString(((Integer) o).intValue()));
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } else if (o instanceof Class) {
            return (Class) o;
        } else {
            throw new RuntimeException("Unknown system ui service: " + o);
        }
    }

    public void onCreate() {
        try {
            this.SERVICES[0] = Integer.valueOf(Stub.asInterface(ServiceManager.getService("window")).hasSystemNavBar() ? 2131296257 : 2131296256);
        } catch (RemoteException e) {
            Slog.w("SystemUIService", "Failing checking whether status bar can hide", e);
        }
        int N = this.SERVICES.length;
        this.mServices = new SystemUI[N];
        int i = 0;
        while (i < N) {
            Class cl = chooseClass(this.SERVICES[i]);
            Slog.d("SystemUIService", "loading: " + cl);
            try {
                this.mServices[i] = (SystemUI) cl.newInstance();
                this.mServices[i].mContext = this;
                Slog.d("SystemUIService", "running: " + this.mServices[i]);
                this.mServices[i].start();
                i++;
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (InstantiationException ex2) {
                throw new RuntimeException(ex2);
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        for (SystemUI ui : this.mServices) {
            ui.onConfigurationChanged(newConfig);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args == null || args.length == 0) {
            for (SystemUI ui : this.mServices) {
                pw.println("dumping service: " + ui.getClass().getName());
                ui.dump(fd, pw, args);
            }
            return;
        }
        String svc = args[0];
        for (SystemUI ui2 : this.mServices) {
            if (ui2.getClass().getName().endsWith(svc)) {
                ui2.dump(fd, pw, args);
            }
        }
    }
}
