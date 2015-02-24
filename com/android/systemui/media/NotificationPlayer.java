package com.android.systemui.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import java.lang.Thread.State;
import java.util.LinkedList;

public class NotificationPlayer implements OnCompletionListener {
    private AudioManager mAudioManager;
    private LinkedList<Command> mCmdQueue;
    private final Object mCompletionHandlingLock;
    private CreationAndCompletionThread mCompletionThread;
    private Looper mLooper;
    private MediaPlayer mPlayer;
    private int mState;
    private String mTag;
    private CmdThread mThread;
    private WakeLock mWakeLock;

    private final class CmdThread extends Thread {
        CmdThread() {
            super("NotificationPlayer-" + NotificationPlayer.this.mTag);
        }

        public void run() {
            while (true) {
                synchronized (NotificationPlayer.this.mCmdQueue) {
                    Command cmd = (Command) NotificationPlayer.this.mCmdQueue.removeFirst();
                }
                switch (cmd.code) {
                    case 1:
                        NotificationPlayer.this.startSound(cmd);
                        break;
                    case 2:
                        if (NotificationPlayer.this.mPlayer != null) {
                            long delay = SystemClock.uptimeMillis() - cmd.requestTime;
                            if (delay > 1000) {
                                Log.w(NotificationPlayer.this.mTag, "Notification stop delayed by " + delay + "msecs");
                            }
                            NotificationPlayer.this.mPlayer.stop();
                            NotificationPlayer.this.mPlayer.release();
                            NotificationPlayer.this.mPlayer = null;
                            NotificationPlayer.this.mAudioManager.abandonAudioFocus(null);
                            NotificationPlayer.this.mAudioManager = null;
                            if (!(NotificationPlayer.this.mLooper == null || NotificationPlayer.this.mLooper.getThread().getState() == State.TERMINATED)) {
                                NotificationPlayer.this.mLooper.quit();
                            }
                        } else {
                            Log.w(NotificationPlayer.this.mTag, "STOP command without a player");
                        }
                        break;
                }
                synchronized (NotificationPlayer.this.mCmdQueue) {
                    if (NotificationPlayer.this.mCmdQueue.size() == 0) {
                        NotificationPlayer.this.mThread = null;
                        NotificationPlayer.this.releaseWakeLock();
                        return;
                    }
                }
            }
        }
    }

    private static final class Command {
        int code;
        Context context;
        boolean looping;
        long requestTime;
        int stream;
        Uri uri;

        private Command() {
        }

        public String toString() {
            return "{ code=" + this.code + " looping=" + this.looping + " stream=" + this.stream + " uri=" + this.uri + " }";
        }
    }

    private final class CreationAndCompletionThread extends Thread {
        public Command mCmd;

        public CreationAndCompletionThread(Command cmd) {
            this.mCmd = cmd;
        }

        public void run() {
            Looper.prepare();
            NotificationPlayer.this.mLooper = Looper.myLooper();
            synchronized (this) {
                AudioManager audioManager = (AudioManager) this.mCmd.context.getSystemService("audio");
                try {
                    MediaPlayer player = new MediaPlayer();
                    player.setAudioStreamType(this.mCmd.stream);
                    player.setDataSource(this.mCmd.context, this.mCmd.uri);
                    player.setLooping(this.mCmd.looping);
                    player.prepare();
                    if (!(this.mCmd.uri == null || this.mCmd.uri.getEncodedPath() == null || this.mCmd.uri.getEncodedPath().length() <= 0)) {
                        if (this.mCmd.looping) {
                            audioManager.requestAudioFocus(null, this.mCmd.stream, 1);
                        } else {
                            audioManager.requestAudioFocus(null, this.mCmd.stream, 3);
                        }
                    }
                    player.setOnCompletionListener(NotificationPlayer.this);
                    player.start();
                    if (NotificationPlayer.this.mPlayer != null) {
                        NotificationPlayer.this.mPlayer.release();
                    }
                    NotificationPlayer.this.mPlayer = player;
                } catch (Exception e) {
                    Log.w(NotificationPlayer.this.mTag, "error loading sound for " + this.mCmd.uri, e);
                }
                NotificationPlayer.this.mAudioManager = audioManager;
                notify();
            }
            Looper.loop();
        }
    }

    private void startSound(Command cmd) {
        try {
            synchronized (this.mCompletionHandlingLock) {
                try {
                    if (!(this.mLooper == null || this.mLooper.getThread().getState() == State.TERMINATED)) {
                        this.mLooper.quit();
                    }
                    this.mCompletionThread = new CreationAndCompletionThread(cmd);
                    synchronized (this.mCompletionThread) {
                        this.mCompletionThread.start();
                        this.mCompletionThread.wait();
                    }
                } catch (Throwable th) {
                }
            }
            long delay = SystemClock.uptimeMillis() - cmd.requestTime;
            if (delay > 1000) {
                Log.w(this.mTag, "Notification sound delayed by " + delay + "msecs");
            }
        } catch (Exception e) {
            Log.w(this.mTag, "error loading sound for " + cmd.uri, e);
        }
    }

    public void onCompletion(MediaPlayer mp) {
        if (this.mAudioManager != null) {
            this.mAudioManager.abandonAudioFocus(null);
        }
        synchronized (this.mCmdQueue) {
            try {
                if (this.mCmdQueue.size() == 0) {
                    synchronized (this.mCompletionHandlingLock) {
                        if (this.mLooper != null) {
                            this.mLooper.quit();
                        }
                        this.mCompletionThread = null;
                    }
                }
            } catch (Throwable th) {
            }
        }
    }

    public NotificationPlayer(String tag) {
        this.mCmdQueue = new LinkedList();
        this.mCompletionHandlingLock = new Object();
        this.mState = 2;
        if (tag != null) {
            this.mTag = tag;
        } else {
            this.mTag = "NotificationPlayer";
        }
    }

    public void play(Context context, Uri uri, boolean looping, int stream) {
        Command cmd = new Command();
        cmd.requestTime = SystemClock.uptimeMillis();
        cmd.code = 1;
        cmd.context = context;
        cmd.uri = uri;
        cmd.looping = looping;
        cmd.stream = stream;
        synchronized (this.mCmdQueue) {
            enqueueLocked(cmd);
            this.mState = 1;
        }
    }

    public void stop() {
        synchronized (this.mCmdQueue) {
            if (this.mState != 2) {
                Command cmd = new Command();
                cmd.requestTime = SystemClock.uptimeMillis();
                cmd.code = 2;
                enqueueLocked(cmd);
                this.mState = 2;
            }
        }
    }

    private void enqueueLocked(Command cmd) {
        this.mCmdQueue.add(cmd);
        if (this.mThread == null) {
            acquireWakeLock();
            this.mThread = new CmdThread();
            this.mThread.start();
        }
    }

    public void setUsesWakeLock(Context context) {
        if (this.mWakeLock == null && this.mThread == null) {
            this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, this.mTag);
            return;
        }
        throw new RuntimeException("assertion failed mWakeLock=" + this.mWakeLock + " mThread=" + this.mThread);
    }

    private void acquireWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
    }
}
