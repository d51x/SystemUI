package com.android.systemui;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemProperties;
import android.renderscript.Matrix4f;
import android.service.wallpaper.WallpaperService;
import android.service.wallpaper.WallpaperService.Engine;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

public class ImageWallpaper extends WallpaperService {
    boolean mIsHwAccelerated;
    WallpaperManager mWallpaperManager;

    class DrawableEngine extends Engine {
        Bitmap mBackground;
        int mBackgroundHeight;
        int mBackgroundWidth;
        private EGL10 mEgl;
        private EGLConfig mEglConfig;
        private EGLContext mEglContext;
        private EGLDisplay mEglDisplay;
        private EGLSurface mEglSurface;
        private GL mGL;
        int mLastXTranslation;
        int mLastYTranslation;
        private final Object mLock;
        boolean mOffsetsChanged;
        private WallpaperObserver mReceiver;
        boolean mRedrawNeeded;
        boolean mVisible;
        float mXOffset;
        float mYOffset;

        class WallpaperObserver extends BroadcastReceiver {
            final /* synthetic */ DrawableEngine this$1;

            public void onReceive(Context context, Intent intent) {
                synchronized (this.this$1.mLock) {
                    DrawableEngine drawableEngine = this.this$1;
                    this.this$1.mBackgroundHeight = -1;
                    drawableEngine.mBackgroundWidth = -1;
                    this.this$1.mBackground = null;
                    this.this$1.mRedrawNeeded = true;
                    this.this$1.drawFrameLocked();
                }
            }
        }

        public DrawableEngine() {
            super(ImageWallpaper.this);
            this.mLock = new Object[0];
            this.mBackgroundWidth = -1;
            this.mBackgroundHeight = -1;
            this.mVisible = true;
            setFixedSizeAllowed(true);
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            updateSurfaceSize(surfaceHolder);
            setOffsetNotificationsEnabled(false);
        }

        public void onDestroy() {
            super.onDestroy();
            if (this.mReceiver != null) {
                ImageWallpaper.this.unregisterReceiver(this.mReceiver);
            }
        }

        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            super.onDesiredSizeChanged(desiredWidth, desiredHeight);
            SurfaceHolder surfaceHolder = getSurfaceHolder();
            if (surfaceHolder != null) {
                updateSurfaceSize(surfaceHolder);
            }
        }

        void updateSurfaceSize(SurfaceHolder surfaceHolder) {
            surfaceHolder.setFixedSize(getDesiredMinimumWidth(), getDesiredMinimumHeight());
        }

        public void onVisibilityChanged(boolean visible) {
            synchronized (this.mLock) {
                if (this.mVisible != visible) {
                    this.mVisible = visible;
                    drawFrameLocked();
                }
            }
        }

        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixels, int yPixels) {
            synchronized (this.mLock) {
                if (!(this.mXOffset == xOffset && this.mYOffset == yOffset)) {
                    this.mXOffset = xOffset;
                    this.mYOffset = yOffset;
                    this.mOffsetsChanged = true;
                }
                drawFrameLocked();
            }
        }

        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            synchronized (this.mLock) {
                this.mRedrawNeeded = true;
                drawFrameLocked();
            }
        }

        void drawFrameLocked() {
            if (!this.mVisible) {
                return;
            }
            if (this.mRedrawNeeded || this.mOffsetsChanged) {
                boolean updateWallpaper;
                if (this.mBackgroundWidth < 0 || this.mBackgroundHeight < 0) {
                    updateWallpaper = true;
                } else {
                    updateWallpaper = false;
                }
                if (updateWallpaper || this.mBackground == null) {
                    updateWallpaper = true;
                } else {
                    updateWallpaper = false;
                }
                if (updateWallpaper) {
                    updateWallpaperLocked();
                }
                SurfaceHolder sh = getSurfaceHolder();
                Rect frame = sh.getSurfaceFrame();
                int availw = frame.width() - this.mBackgroundWidth;
                int availh = frame.height() - this.mBackgroundHeight;
                int xPixels = availw < 0 ? (int) ((((float) availw) * this.mXOffset) + 0.5f) : availw / 2;
                int yPixels = availh < 0 ? (int) ((((float) availh) * this.mYOffset) + 0.5f) : availh / 2;
                this.mOffsetsChanged = false;
                if (this.mRedrawNeeded || xPixels != this.mLastXTranslation || yPixels != this.mLastYTranslation) {
                    this.mRedrawNeeded = false;
                    this.mLastXTranslation = xPixels;
                    this.mLastYTranslation = yPixels;
                    if (!ImageWallpaper.this.mIsHwAccelerated) {
                        drawWallpaperWithCanvas(sh, availw, availh, xPixels, yPixels);
                    } else if (!drawWallpaperWithOpenGL(sh, availw, availh, xPixels, yPixels)) {
                        drawWallpaperWithCanvas(sh, availw, availh, xPixels, yPixels);
                    }
                    this.mBackground = null;
                    ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                }
            }
        }

        void updateWallpaperLocked() {
            int width;
            int i = 0;
            Throwable exception = null;
            try {
                this.mBackground = ImageWallpaper.this.mWallpaperManager.getBitmap();
            } catch (Throwable e) {
                exception = e;
            } catch (Throwable e2) {
                exception = e2;
            }
            if (exception != null) {
                this.mBackground = null;
                Log.w("ImageWallpaper", "Unable to load wallpaper!", exception);
                try {
                    ImageWallpaper.this.mWallpaperManager.clear();
                } catch (IOException ex) {
                    Log.w("ImageWallpaper", "Unable reset to default wallpaper!", ex);
                }
            }
            if (this.mBackground != null) {
                width = this.mBackground.getWidth();
            } else {
                width = 0;
            }
            this.mBackgroundWidth = width;
            if (this.mBackground != null) {
                i = this.mBackground.getHeight();
            }
            this.mBackgroundHeight = i;
        }

        private void drawWallpaperWithCanvas(SurfaceHolder sh, int w, int h, int x, int y) {
            Canvas c = sh.lockCanvas();
            if (c != null) {
                c.translate((float) x, (float) y);
                if (w < 0 || h < 0) {
                    c.save(2);
                    c.clipRect(0.0f, 0.0f, (float) this.mBackgroundWidth, (float) this.mBackgroundHeight, Op.DIFFERENCE);
                    c.drawColor(-16777216);
                    c.restore();
                }
                if (this.mBackground != null) {
                    c.drawBitmap(this.mBackground, 0.0f, 0.0f, null);
                }
                sh.unlockCanvasAndPost(c);
            }
        }

        private boolean drawWallpaperWithOpenGL(SurfaceHolder sh, int w, int h, int left, int top) {
            if (!initGL(sh)) {
                return false;
            }
            float right = (float) (this.mBackgroundWidth + left);
            float bottom = (float) (this.mBackgroundHeight + top);
            Rect frame = sh.getSurfaceFrame();
            Matrix4f ortho = new Matrix4f();
            ortho.loadOrtho(0.0f, (float) frame.width(), (float) frame.height(), 0.0f, -1.0f, 1.0f);
            Buffer triangleVertices = createMesh(left, top, right, bottom);
            int texture = loadTexture(this.mBackground);
            int program = buildProgram("attribute vec4 position;\nattribute vec2 texCoords;\nvarying vec2 outTexCoords;\nuniform mat4 projection;\n\nvoid main(void) {\n    outTexCoords = texCoords;\n    gl_Position = projection * position;\n}\n\n", "precision mediump float;\n\nvarying vec2 outTexCoords;\nuniform sampler2D texture;\n\nvoid main(void) {\n    gl_FragColor = texture2D(texture, outTexCoords);\n}\n\n");
            int attribPosition = GLES20.glGetAttribLocation(program, "position");
            int attribTexCoords = GLES20.glGetAttribLocation(program, "texCoords");
            int uniformTexture = GLES20.glGetUniformLocation(program, "texture");
            int uniformProjection = GLES20.glGetUniformLocation(program, "projection");
            checkGlError();
            GLES20.glViewport(0, 0, frame.width(), frame.height());
            GLES20.glBindTexture(3553, texture);
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(attribPosition);
            GLES20.glEnableVertexAttribArray(attribTexCoords);
            GLES20.glUniform1i(uniformTexture, 0);
            GLES20.glUniformMatrix4fv(uniformProjection, 1, false, ortho.getArray(), 0);
            checkGlError();
            if (w < 0 || h < 0) {
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glClear(16384);
            }
            triangleVertices.position(0);
            GLES20.glVertexAttribPointer(attribPosition, 3, 5126, false, 20, triangleVertices);
            triangleVertices.position(3);
            GLES20.glVertexAttribPointer(attribTexCoords, 3, 5126, false, 20, triangleVertices);
            GLES20.glDrawArrays(5, 0, 4);
            if (this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface)) {
                checkEglError();
                finishGL();
                return true;
            }
            throw new RuntimeException("Cannot swap buffers");
        }

        private FloatBuffer createMesh(int left, int top, float right, float bottom) {
            float[] verticesData = new float[]{(float) left, bottom, 0.0f, 0.0f, 1.0f, right, bottom, 0.0f, 1.0f, 1.0f, (float) left, (float) top, 0.0f, 0.0f, 0.0f, right, (float) top, 0.0f, 1.0f, 0.0f};
            FloatBuffer triangleVertices = ByteBuffer.allocateDirect(verticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(verticesData).position(0);
            return triangleVertices;
        }

        private int loadTexture(Bitmap bitmap) {
            int[] textures = new int[1];
            GLES20.glActiveTexture(33984);
            GLES20.glGenTextures(1, textures, 0);
            checkGlError();
            int texture = textures[0];
            GLES20.glBindTexture(3553, texture);
            checkGlError();
            GLES20.glTexParameteri(3553, 10241, 9729);
            GLES20.glTexParameteri(3553, 10240, 9729);
            GLES20.glTexParameteri(3553, 10242, 33071);
            GLES20.glTexParameteri(3553, 10243, 33071);
            GLUtils.texImage2D(3553, 0, 6408, bitmap, 5121, 0);
            checkGlError();
            bitmap.recycle();
            return texture;
        }

        private int buildProgram(String vertex, String fragment) {
            int vertexShader = buildShader(vertex, 35633);
            if (vertexShader == 0) {
                return 0;
            }
            int fragmentShader = buildShader(fragment, 35632);
            if (fragmentShader == 0) {
                return 0;
            }
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            checkGlError();
            GLES20.glAttachShader(program, fragmentShader);
            checkGlError();
            GLES20.glLinkProgram(program);
            checkGlError();
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, 35714, status, 0);
            if (status[0] == 1) {
                return program;
            }
            Log.d("ImageWallpaperGL", "Error while linking program:\n" + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            GLES20.glDeleteProgram(program);
            return 0;
        }

        private int buildShader(String source, int type) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            checkGlError();
            GLES20.glCompileShader(shader);
            checkGlError();
            int[] status = new int[1];
            GLES20.glGetShaderiv(shader, 35713, status, 0);
            if (status[0] == 1) {
                return shader;
            }
            Log.d("ImageWallpaperGL", "Error while compiling shader:\n" + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        private void checkEglError() {
            int error = this.mEgl.eglGetError();
            if (error != 12288) {
                Log.w("ImageWallpaperGL", "EGL error = " + GLUtils.getEGLErrorString(error));
            }
        }

        private void checkGlError() {
            int error = GLES20.glGetError();
            if (error != 0) {
                Log.w("ImageWallpaperGL", "GL error = 0x" + Integer.toHexString(error), new Throwable());
            }
        }

        private void finishGL() {
            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            this.mEgl.eglDestroySurface(this.mEglDisplay, this.mEglSurface);
            this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
        }

        private boolean initGL(SurfaceHolder surfaceHolder) {
            this.mEgl = (EGL10) EGLContext.getEGL();
            this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
            if (this.mEgl.eglInitialize(this.mEglDisplay, new int[2])) {
                this.mEglConfig = chooseEglConfig();
                if (this.mEglConfig == null) {
                    throw new RuntimeException("eglConfig not initialized");
                }
                this.mEglContext = createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
                this.mEglSurface = this.mEgl.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig, surfaceHolder, null);
                if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                    int error = this.mEgl.eglGetError();
                    if (error == 12299) {
                        Log.e("ImageWallpaperGL", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                        return false;
                    }
                    throw new RuntimeException("createWindowSurface failed " + GLUtils.getEGLErrorString(error));
                } else if (this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                    this.mGL = this.mEglContext.getGL();
                    return true;
                } else {
                    throw new RuntimeException("eglMakeCurrent failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
                }
            }
            throw new RuntimeException("eglInitialize failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }

        EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, 12344});
        }

        private EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            if (this.mEgl.eglChooseConfig(this.mEglDisplay, getConfig(), configs, 1, configsCount)) {
                return configsCount[0] > 0 ? configs[0] : null;
            } else {
                throw new IllegalArgumentException("eglChooseConfig failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
        }

        private int[] getConfig() {
            return new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12344};
        }
    }

    public void onCreate() {
        super.onCreate();
        this.mWallpaperManager = (WallpaperManager) getSystemService("wallpaper");
        if (!isEmulator()) {
            this.mIsHwAccelerated = ActivityManager.isHighEndGfx(((WindowManager) getSystemService("window")).getDefaultDisplay());
        }
    }

    private static boolean isEmulator() {
        return "1".equals(SystemProperties.get("ro.kernel.qemu", "0"));
    }

    public Engine onCreateEngine() {
        return new DrawableEngine();
    }
}
