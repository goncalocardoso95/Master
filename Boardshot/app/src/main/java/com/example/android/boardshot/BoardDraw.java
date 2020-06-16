package com.example.android.boardshot;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class BoardDraw extends AppCompatActivity {

    private int left;
    private int top;
    private int width;
    private int height;

    private TextView textView;
    private String colorCode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_draw);

        Bundle bundle = getIntent().getExtras();
        String message = bundle.getString("message");
        String[] messageArray = message.split(" ");
        textView = findViewById(R.id.tvRobot);


         left = 50; // initial start position of rectangles (50 pixels from left)
         top = 50; // 50 pixels from the top
         width = 80;
         height = 80;
        Bitmap bg = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
        int index = 0;
        int col;
        for (int row = 0; row < 8; row++) { // draw 2 rows
            for(col = 0; col < 6; col++) { // draw 4 columns
                Paint paint = new Paint();
                if (messageArray[index].contains("O")){

                    Log.d("AA","O"+index);
                    paint.setColor(Color.parseColor("#008000"));

                }else if(messageArray[index].contains("X")){
                    Log.d("AA","X"+index);
                    paint.setColor(Color.parseColor("#CD5C5C"));

                }else if(messageArray[index].contains("F")) {
                    Log.d("AA","F"+index);
                    paint.setColor(Color.parseColor("#3792cb"));

                }else if(messageArray[index].contains("R")) {
                    Log.d("AA", "R"+index);
                    paint.setColor(Color.parseColor("#000000"));
                }


                Canvas canvas = new Canvas(bg);
                canvas.drawRect(left, top, left+width, top+height, paint);
                left = (left + width  +10); // set new left co-ordinate + 10 pixel gap
                // Do other things here
                // i.e. change colour
                index++;
            }

            top = top + height + 10; // move to new row by changing the top co-ordinate
            left = 50;
        }

        RelativeLayout ll = (RelativeLayout) findViewById(R.id.rect);
        ImageView iV = new ImageView(this);
        iV.setImageBitmap(bg);
        ll.addView(iV);


        /*Bitmap big = Bitmap.createBitmap(5000, 5000, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(big);
        paint.setColor(Color.parseColor("#ff0000"));
        canvas.drawRect(left, top+1000, left+width, top+height+1000, paint);
        ImageView iV2 = new ImageView(this);
        iV2.setImageBitmap(big);
        ll.addView(iV2);
        */




        try{
            File file = new File( Environment.getExternalStorageDirectory().toString()+"/Pictures/"+"test.ozopy");
            FileWriter writer = new FileWriter(file);
            writer.append("def f():\n");
            writer.append("\t move(30,30) \n");
            writer.append("\t move(30,30) \n");
            writer.append("\t wheels(0, 0) \n ");
            writer.append("\n");
            writer.append("i = 0 \n");
            writer.append("i = 0 \n");
            writer.append("i = 0 \n");
            writer.append("while i < 1: \n");
            writer.append("\t f()\n");
            writer.append("i = i + 1 ");
            writer.flush();
            writer.close();

            RandomAccessFile f = new RandomAccessFile(file, "r");
            byte[] b = new byte[(int)f.length()];
            f.readFully(b);
            OutputStream outputStream2 = null;
            try {
                outputStream2 = new FileOutputStream(file);
                outputStream2.write(b);


            } finally {
                if (outputStream2 != null)
                    outputStream2.close();
            }
            

        }catch (Exception e){
            e.printStackTrace();

        }
        Python py= Python.getInstance();
        PyObject initModule = py.getModule("Runnable");
        PyObject runCall= initModule.callAttr("test",Environment.getExternalStorageDirectory().toString()+"/Pictures/"+"test.ozopy");
        colorCode = runCall.toString();
        Toast.makeText(getApplicationContext(),"ANTES BUTTON = "+colorCode,Toast.LENGTH_LONG).show();




        Button b = findViewById(R.id.loadRobot);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Toast.makeText(getApplicationContext(),runCall.toString(),Toast.LENGTH_LONG).show();

                Map<String,String> colormap = new HashMap<>();
                colormap.put("k","#000000");
                colormap.put("R","#ff0000");
                colormap.put("G","#00ff00");
                colormap.put("Y","#ffff00");
                colormap.put("B","#0000ff");
                colormap.put("M","#ff00ff");
                colormap.put("C","#00ffff");
                colormap.put("W","#ffffff");
                Toast.makeText(getApplicationContext(),"DEPOIS BUTTON = "+colorCode,Toast.LENGTH_LONG).show();
                Log.d("COLOR",colorCode);

                int colorFrom1 = Color.parseColor("#000000");
                Log.d("TEST",1+"");
                int colorFrom2 = Color.parseColor("#ff0000");
                Log.d("TEST",2+"");
                int colorFrom3 = Color.parseColor("#00ff00");
                Log.d("TEST",3+"");
                int colorFrom4 = Color.parseColor("#ffff00");
                Log.d("TEST",4+"");
                int colorFrom5= Color.parseColor("#0000ff");
                Log.d("TEST",5+"");
                int colorFrom6= Color.parseColor("#ff00ff");
                Log.d("TEST",6+"");
                int colorFrom7= Color.parseColor("#00ffff");
                Log.d("TEST",7+"");
                int colorFrom8= Color.parseColor("#ffffff");
                Log.d("TEST",8+"");

                ValueAnimator colorAnimation =null ;
                for (int i = 1; i < colorCode.length(); i+=2) {
                     int colorFrom = Color.parseColor(String.valueOf(colormap.get(colorCode.charAt(i-1))));
                     int colorTo = Color.parseColor(String.valueOf(colormap.get(colorCode.charAt(i))));
                     colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                     colorAnimation.setDuration(50); // milliseconds
                     colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            textView.setBackgroundColor((int) animator.getAnimatedValue());
                        }

                    });
                }

                colorAnimation.start();




             /*   for (int i = 1; i < colorCode.length(); i+=2){
                    Log.d("TEST",i+"");
                    int colorFrom = Color.parseColor(String.valueOf(colormap.get(colorCode.charAt(i-1))));
                    int colorTo = Color.parseColor(String.valueOf(colormap.get(colorCode.charAt(i))));
                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(50); // milliseconds
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            textView.setBackgroundColor((int) animator.getAnimatedValue());
                        }

                    });
                    colorAnimation.start();

                }*/



            }
        });



    }




}
