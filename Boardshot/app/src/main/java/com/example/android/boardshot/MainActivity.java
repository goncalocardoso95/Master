package com.example.android.boardshot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Sampler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.chaquo.python.*;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends AppCompatActivity {


    private Button btnCapture;
    private Button btnCaptureWithPieces;
    private Button btnToturial;
    private TextureView textureView;

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    //Save to FILE
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private Button camera;
    private Button recognizeBlocks;
    private TextView textViewCodes;
    private TextToSpeech ts;
    private Timer timer;
    private TimerTask repeatedTask;

    private int start;

    private static final String CAMERA_PATH = "/storage/emulated/0/";
    private static final String CAMERA_PATH_PHONE = "/Memória/DCIM/Camera";

    private TextToSpeech engine;

    private DatabaseReference database;
    private static final int THRESHOLD = 150;
    private int recPieces = 0;
    private int board = 0;
    private int tutorialPieces = 0;
    private String instruction = "";
    private String sequenceDB = "";
    private boolean hasFinal =false;

    private Python py;
    private String rec;





    private StorageReference mStorageRef;

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice=null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initDebug()){

            Toast.makeText(getApplicationContext(),"sucesso",Toast.LENGTH_LONG).show();

        }else{
            Toast.makeText(getApplicationContext(),"Insucesso",Toast.LENGTH_LONG).show();

        }



        //PyObject pyf = py.getModule("parameter");
        //PyObject obj = pyf.callAttr("passParameter","WOW");

        //Toast.makeText(getApplicationContext(),obj.toString(),Toast.LENGTH_LONG).show();




        database = FirebaseDatabase.getInstance().getReference("");
        mStorageRef = FirebaseStorage.getInstance().getReference();

        textureView = (TextureView)findViewById(R.id.textureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        btnCapture = (Button)findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                takePicture();
                board = 1;

            }
        });

        btnCaptureWithPieces = (Button)findViewById(R.id.btnCapturePieces);
        btnCaptureWithPieces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 repeatedTask = new TimerTask() {
                    @Override
                    public void run() {

                        takePicture();
                    }
                };
                timer = new Timer("Timer");
                long delay = 1;
                long period = 5000;
                timer.scheduleAtFixedRate(repeatedTask, delay, period);
                recPieces = 1;
            }
        });

        btnToturial = (Button)findViewById(R.id.Tutorial);
        btnToturial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
                tutorialPieces = 1;
            }
        });






    }

    private void takePicture() {
        if(cameraDevice == null)
            return;
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            //Capture image with custom size
            int width = 640;
            int height = 480;
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //Check orientation base on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

            file = new File(Environment.getExternalStorageDirectory()+"/"+UUID.randomUUID().toString()+".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try{
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);

                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally {
                        {
                            if(image != null)
                                image.close();

                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);


                    } finally {
                        if (outputStream != null)
                            outputStream.close();
                    }


                    if (!Python.isStarted()){
                        Python.start(new AndroidPlatform(getApplicationContext()));
                    }

                    if (Python.isStarted()){
                        Toast.makeText(getApplicationContext(),"Python Started",Toast.LENGTH_LONG).show();
                    }

                    Python py = Python.getInstance();
                    PyObject cv2 = py.getModule("cv2");
                    PyObject numpy = py.getModule("numpy");


                    PyObject tab = py.getModule("BoardRecognition");
                    PyObject obj= tab.callAttr("boardRecognition",file.getPath());
                    List<PyObject> fourCorners = obj.asList();

                    int x1 = fourCorners.get(0).asList().get(0).toInt();
                    int y1 = fourCorners.get(0).asList().get(1).toInt();
                    int x2 = fourCorners.get(0).asList().get(2).toInt();
                    int y2 = fourCorners.get(0).asList().get(3).toInt();

                    Log.d("corners",x1+" "+y1+" "+x2+" "+y2);

                    Mat matrix = Imgcodecs.imread(file.getPath());


                    //hsv
                    //Imgproc.cvtColor(matrix,matrix,Imgproc.COLOR_BGR2HSV);

                    List<MatOfPoint> contours = new ArrayList<>();

                    //Mat hierachy = new Mat();
                    //Imgproc.findContours(matrix, contours, matrix, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                    //Imgproc.rectangle(matrix,new Point(x1, y1),new Point(x2, y2),new Scalar(0, 0, 255),10);

                    PyObject vhlines = py.getModule("verticalAndHorizontal");
                    PyObject obj1= vhlines.callAttr("boardRecognition",file.getPath());
                    List<PyObject> Horizontal = obj1.asList();


                    for (int i = 0;i < Horizontal.size();i++){


                        int Hy1 = Horizontal.get(i).toInt();

                        //Imgproc.rectangle(matrix,new Point(x1, Hy1),new Point(x2, Hy1),new Scalar(0, 0, 255),10);
                        //Toast.makeText(getApplicationContext(),verticalAndHorizontal.get(i).toInt()+"",Toast.LENGTH_LONG).show();
                    }


                    PyObject obj2= vhlines.callAttr("verticalLines",file.getPath());
                    List<PyObject> vertical = obj2.asList();


                    for (int i = 0;i < vertical.size();i++){


                        int Hx1 = vertical.get(i).toInt();

                        //Imgproc.rectangle(matrix,new Point(Hx1, y1),new Point(Hx1, y2),new Scalar(0, 0, 255),10);
                        //Toast.makeText(getApplicationContext(),verticalAndHorizontal.get(i).toInt()+"",Toast.LENGTH_LONG).show();
                    }

                    TextView tv = findViewById(R.id.textoo);
                    PyObject obj3= vhlines.callAttr("squares",file.getPath());
                    List<PyObject> squares = obj3.asList();

                    //brigth image
                    Mat matrixBright = new Mat();
                    matrix.convertTo(matrixBright, -1, 1, 200);

                    Mat matrixRobot = new Mat();
                    matrix.convertTo(matrixRobot, -1, 1, 35);



                    Mat cropped= null;
                    Mat croppedNormal= null;
                    rec = "";
                    int count = 0;
                    Map<String,Integer> blackareas = new HashMap();
                    Map<String,String> tabuleiro = new HashMap();


                    Toast.makeText(getApplicationContext(),squares.size()+"",Toast.LENGTH_LONG).show();
                    int linha = 0;
                    for (int i = 0;i < squares.size();i++){

                        int a = squares.get(i).asList().get(0).toInt();
                        int b = squares.get(i).asList().get(1).toInt();
                        int c = squares.get(i).asList().get(2).toInt();
                        int d = squares.get(i).asList().get(3).toInt();
                        //Log.d("Tag",a+" "+b+" "+c+" "+d);


                        Rect roi = new Rect(a, b,c - a , d - b);
                        cropped = new Mat(matrixBright, roi);
                        croppedNormal = new Mat(matrix, roi);



                        int w = c - a, h = d - b;


                        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                        Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
                        Canvas canvas = new Canvas(bmp);
                        Utils.matToBitmap(cropped,bmp);

                        Bitmap.Config conf2 = Bitmap.Config.ARGB_8888; // see other conf types
                        Bitmap bmp2 = Bitmap.createBitmap(w, h, conf2); // this creates a MUTABLE bitmap
                        Canvas canvas2 = new Canvas(bmp2);
                        Utils.matToBitmap(croppedNormal,bmp2);


                        int redColors = 0;
                        int greenColors = 0;
                        int blueColors = 0;
                        int pixelCount = 0;

                        for (int y = 0; y < bmp.getHeight(); y++)
                        {
                            for (int x = 0; x < bmp.getWidth(); x++)
                            {
                                int color = bmp.getPixel(x, y);
                                pixelCount++;
                                redColors += Color.red(color);
                                greenColors += Color.green(color);
                                blueColors += Color.blue(color);


                            }
                        }


                        int red = (redColors/pixelCount);
                        int green = (greenColors/pixelCount);
                        int blue = (blueColors/pixelCount);
                        //int yellow = ((redColors+greenColors)/pixelCount);

                        if(green >= red && green >=  blue){
                            tabuleiro.put(""+linha+""+count,"O");
                        }

                        else if (red >= green && red >= blue){
                            tabuleiro.put(""+linha+""+count,"F");
                        }

                        else if(blue >= red && blue >= green){
                            tabuleiro.put(""+linha+""+count,"X");
                        }



                        MatOfByte matOfByte = new MatOfByte();
                        Imgcodecs.imencode(".jpg", croppedNormal, matOfByte);
                        byte[] byteArray = matOfByte.toArray();

                        //save method
                        OutputStream outputStream2 = null;
                        try {
                            outputStream2 = new FileOutputStream(file);
                            outputStream2.write(byteArray);


                        } finally {
                            if (outputStream2 != null)
                                outputStream2.close();
                        }




                        //PyObject blackLines = py.getModule("BlackLines");
                        //PyObject blackLinesMod = blackLines.callAttr("getBlackCounts",file.getPath());
                        //int black = blackLinesMod.toInt();
                        //Log.d("BlackArea",""+linha+""+count+","+black+"");
                        //blackareas.put(""+linha+""+count,black);



                        count++;
                        if (count == 6){
                            rec+="\n";
                            count = 0;
                            linha++;
                        }

                    }
                    //String robotCoordenates = getRobotCoordenates(blackareas);
                    //tabuleiro.put(robotCoordenates,"R");
                    rec= "";
                        for (int i = 0;i < 8;i++){

                            for (int j = 0;j < 6;j++){
                                rec+= tabuleiro.get(""+i+""+j)+" ";


                            }
                            rec+="\n";

                        }






                    MatOfByte matOfByte = new MatOfByte();
                    Imgcodecs.imencode(".jpg", matrix, matOfByte);
                    byte[] byteArray = matOfByte.toArray();

                    //save method
                    OutputStream outputStream2 = null;
                    try {
                        outputStream2 = new FileOutputStream(file);
                        outputStream2.write(byteArray);


                    } finally {
                        if (outputStream2 != null)
                            outputStream2.close();
                    }




                    tv.setText(rec);


                    Mat gray = new Mat();
                    Imgproc.cvtColor(matrix,gray,Imgproc.COLOR_RGB2GRAY,20);










                    if (recPieces == 1) {
                        Toast.makeText(MainActivity.this, "recPieces", Toast.LENGTH_SHORT).show();
                        List<TopCode> listaTopCodes = recognizeTopcodes(file.getPath());
                        if (!listaTopCodes.isEmpty() && hasFinalCode(listaTopCodes)) {
                            hasFinal = true;
                            Toast.makeText(MainActivity.this, "!Empty && final code", Toast.LENGTH_SHORT).show();
                            for (Integer t : instructionsRecognition(listaTopCodes)) {

                                if (t == 31) {
                                    sequenceDB += "E_";
                                } else if (t == 47) {
                                    sequenceDB += "D_";
                                } else if (t == 55) {
                                    sequenceDB += "C_";
                                } else if (t == 59) {
                                    sequenceDB += "B_";
                                } else if (t == 107) {
                                    sequenceDB += "F";
                                    Toast.makeText(MainActivity.this, "FIMMMMMMM", Toast.LENGTH_SHORT).show();

                                }
                            }
                        }


                    }
                    if (tutorialPieces == 1) {
                        tutorialPieces = 0;
                        List<TopCode> listaTopCodes = recognizeTopcodes(file.getPath());
                        if (!listaTopCodes.isEmpty()) {
                            for (TopCode t : listaTopCodes) {
                                Toast.makeText(MainActivity.this, "Topcode = "+t.code, Toast.LENGTH_SHORT).show();
                                if (t.code == 31) {
                                    instruction = "Esquerda";

                                } else if (t.code == 47) {
                                    instruction = "Direita";

                                } else if (t.code == 55) {
                                    instruction = "Cima";

                                } else if (t.code == 59) {
                                    instruction = "Baixo";

                                } else if (t.code == 107) {
                                    instruction = "Fim";

                                }
                            }

                            engine = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    if (status == TextToSpeech.SUCCESS){
                                        engine.setLanguage(new Locale("pt", "PT"));
                                        engine.speak("A peça corresponde à instrução "+instruction,
                                                TextToSpeech.QUEUE_FLUSH, null, null);
                                    }

                                }
                            });




                        }
                    }

                    if (recPieces == 1 || board == 1) {

                        if (recPieces ==1 && hasFinal){
                            timer.cancel();
                            repeatedTask.cancel();
                            engine = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    engine.setLanguage(new Locale("pt", "PT"));
                                    engine.speak("Sequencia reconhecida",
                                            TextToSpeech.QUEUE_FLUSH, null, null);
                                }
                            });
                            database.child("sequencia").setValue(sequenceDB);
                            recPieces = 0;
                            sequenceDB ="";
                        }


                        board = 0;

                        Toast.makeText(MainActivity.this, "saved", Toast.LENGTH_SHORT).show();

                        Uri filed = Uri.fromFile(file);

                        StorageReference riversRef = mStorageRef.child("images");
                        Toast.makeText(MainActivity.this, "chego aqui", Toast.LENGTH_SHORT).show();

                        riversRef.putFile(filed)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // Get a URL to the uploaded content
                                        //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                        Toast.makeText(MainActivity.this, "image Uploaded", Toast.LENGTH_SHORT).show();

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle unsuccessful uploads
                                        // ...
                                        Toast.makeText(MainActivity.this, "image Upload falhou", Toast.LENGTH_SHORT).show();
                                    }
                                });


                        Handler mHandler = new Handler(getMainLooper());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, BoardDraw.class);
                                intent.putExtra("message", rec);
                                startActivity(intent);
                            }
                        });



                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved "+file, Toast.LENGTH_SHORT).show();

                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId,stateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread= null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }




    private void deleteAllPhotos(){
        File cameraFolder = new File(CAMERA_PATH);

        if (cameraFolder.isDirectory()){
            File[] photos = cameraFolder.listFiles();
            if(photos != null) {
                String concatNameFiles = "";
                for (File image : photos) {
                    image.delete();

                }

            }else{
                Toast.makeText(getApplicationContext(), "Não contém fotos", Toast.LENGTH_LONG).show();
            }

        }

    }


    private String getLastPhotoTaken(){

        File cameraFolder = new File(CAMERA_PATH);

        Map<String,Date> photoDates = new HashMap<>();

        if (cameraFolder.isDirectory()){
            File[] photos = cameraFolder.listFiles();
            if(photos != null) {
                String concatNameFiles = "";
                for (File image : photos) {
                    Date date = new Date(image.lastModified());
                    photoDates.put(image.getName(),date);
                }


            }else{
                Toast.makeText(getApplicationContext(), "Não contém fotos", Toast.LENGTH_LONG).show();
            }

        }

        return getLastName(photoDates);
    }



    private static String getLastName(@NonNull Map<String, Date> photoDates){

        String lastPhoto = "";
        Date lastDate = new Date("05/05/1970");
        for (Map.Entry<String,Date> entry1 : photoDates.entrySet()) {
            if (!(entry1.getKey().equals("cache")) && entry1.getValue().after(lastDate) ){
                lastDate = entry1.getValue();
                lastPhoto = entry1.getKey();
            }
        }
        return  lastPhoto;
    }



    private List<TopCode> recognizeTopcodes(String nameLastPhoto){
        Toast.makeText(getApplicationContext(),"path ="+ nameLastPhoto, Toast.LENGTH_LONG).show();

        List<TopCode> lista = null;
        Bitmap bmImg = BitmapFactory.decodeFile(nameLastPhoto);
        if (bmImg != null) {
            Scanner sc = new Scanner();
            lista = sc.scan(bmImg);
            if (lista.isEmpty())
                Toast.makeText(getApplicationContext(), "lista de scan vazia", Toast.LENGTH_LONG).show();
            String codes = "";
            for (TopCode t : lista) {
                codes += t.getCode() + " "+"x = "+t.getCenterX()+ " "+"y = "+t.getCenterY()+"\n";
            }
            //Toast.makeText(getApplicationContext(), codes, Toast.LENGTH_LONG).show();
        }else{

            Toast.makeText(getApplicationContext(), "Não tem fotos tiradas ou luz pode não permitir o reconhecimento do topcode", Toast.LENGTH_LONG).show();

        }
        return  lista;
    }

    private boolean hasFinalCode(List<TopCode> lt){

        for (TopCode t : lt){

            if (t.getCode() == 107 ){
                return true;
            }
        }

        return false;
    }

    private List<Integer> instructionsRecognition(List<TopCode> listaTopCodes){

        String finalInstructions= "";
        List<Float> sortYCoordenate = new ArrayList<>();
        List<Integer> TopcodeInstructions = new ArrayList<>();
        if (listaTopCodes!= null) {
            for (TopCode t : listaTopCodes) {
                sortYCoordenate.add(t.getCenterY());

            }
            Collections.sort(sortYCoordenate);
            for (float f : sortYCoordenate){
                for (TopCode t:listaTopCodes){
                    if (f == t.getCenterY()){
                        TopcodeInstructions.add(t.getCode());
                    }
                }
            }


        }else{
            Toast.makeText(getApplicationContext(), "a lista está vazia", Toast.LENGTH_LONG).show();

        }


        return TopcodeInstructions;
    }
    private void printMatrixAdjusted(List<PyObject> matrixAdjusted) {
        for (PyObject py : matrixAdjusted){
            Log.d("Matrix", py.asList()+"");
        }

    }

    private String getRobotCoordenates(Map<String, Integer> blackareas) {

        List<Integer> list = new ArrayList<>(blackareas.values());
        Collections.sort(list);
        int value = list.get(list.size() - 1);
        for (Map.Entry<String, Integer> entry : blackareas.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }

        }
        return "";

    }


}
