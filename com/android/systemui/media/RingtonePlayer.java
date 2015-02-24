package com.android.systemui.media;

import android.media.IAudioService;
import android.media.IRingtonePlayer;
import android.media.IRingtonePlayer.Stub;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.systemui.SystemUI;
import com.google.android.collect.Maps;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class RingtonePlayer extends SystemUI {
    private final NotificationPlayer mAsyncPlayer;
    private IAudioService mAudioService;
    private IRingtonePlayer mCallback;
    private final HashMap<IBinder, Client> mClients;

    private class Client implements DeathRecipient {
        private final Ringtone mRingtone;
        private final IBinder mToken;

        public Client(IBinder token, Uri uri, int streamType) {
            this.mToken = token;
            this.mRingtone = new Ringtone(RingtonePlayer.this.mContext, false);
            this.mRingtone.setStreamType(streamType);
            this.mRingtone.setUri(uri);
        }

        public void binderDied() {
            synchronized (RingtonePlayer.this.mClients) {
                RingtonePlayer.this.mClients.remove(this.mToken);
            }
            this.mRingtone.stop();
        }
    }

    public RingtonePlayer() {
        this.mAsyncPlayer = new NotificationPlayer("RingtonePlayer");
        this.mClients = Maps.newHashMap();
        this.mCallback = new Stub() {
            public void play(IBinder token, Uri uri, int streamType) throws RemoteException {
                Client client;
                synchronized (RingtonePlayer.this.mClients) {
                    client = (Client) RingtonePlayer.this.mClients.get(token);
                    if (client == null) {
                        client = new Client(token, uri, streamType);
                        token.linkToDeath(client, 0);
                        RingtonePlayer.this.mClients.put(token, client);
                    }
                }
                client.mRingtone.play();
            }

            public void stop(IBinder token) {
                synchronized (RingtonePlayer.this.mClients) {
                    Client client = (Client) RingtonePlayer.this.mClients.remove(token);
                }
                if (client != null) {
                    client.mToken.unlinkToDeath(client, 0);
                    client.mRingtone.stop();
                }
            }

            public boolean isPlaying(IBinder token) {
                Client client;
                synchronized (RingtonePlayer.this.mClients) {
                    client = (Client) RingtonePlayer.this.mClients.get(token);
                }
                return client != null ? client.mRingtone.isPlaying() : false;
            }

            public void playAsync(Uri uri, boolean looping, int streamType) {
                if (Binder.getCallingUid() != 1000) {
                    throw new SecurityException("Async playback only available from system UID.");
                }
                RingtonePlayer.this.mAsyncPlayer.play(RingtonePlayer.this.mContext, uri, looping, streamType);
            }

            public void stopAsync() {
                if (Binder.getCallingUid() != 1000) {
                    throw new SecurityException("Async playback only available from system UID.");
                }
                RingtonePlayer.this.mAsyncPlayer.stop();
            }
        };
    }

    public void start() {
        this.mAsyncPlayer.setUsesWakeLock(this.mContext);
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        try {
            this.mAudioService.setRingtonePlayer(this.mCallback);
        } catch (RemoteException e) {
            Slog.e("RingtonePlayer", "Problem registering RingtonePlayer: " + e);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Clients:");
        synchronized (this.mClients) {
            for (Client client : this.mClients.values()) {
                pw.print("  mToken=");
                pw.print(client.mToken);
                pw.print(" mUri=");
                pw.println(client.mRingtone.getUri());
            }
        }
    }
}
