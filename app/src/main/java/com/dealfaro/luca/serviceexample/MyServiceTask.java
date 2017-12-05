package com.dealfaro.luca.serviceexample;

import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.view.Menu;
import android.view.MenuItem;

import android.provider.Settings;
import android.os.Bundle;
import android.widget.TextView;
import java.lang.Math;


import static android.content.Context.SENSOR_SERVICE;



/* -------------------------------------------------------------------------

Used Luca's existing Services and NotifyResultCallback to run a service after 30 seconds of wait





------------------------------------------------------------------------------ */

public class MyServiceTask implements Runnable, SensorEventListener {

    public static final String LOG_TAG = "MyService";
    private boolean running;
    private Context context;

    private Sensor mySensor;
    private SensorManager SM;
    private int didItMove = 0;
    Date initDate, currentDate;


    private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(
            new HashSet<ResultCallback>());
    private ConcurrentLinkedQueue<ServiceResult> freeResults =
            new ConcurrentLinkedQueue<ServiceResult>();

    public MyServiceTask(Context _context) {
        context = _context;
        // Put here what to do at creation.
    }


    @Override
    public void run() {


        initDate = Calendar.getInstance().getTime();
        Date tempDate = Calendar.getInstance().getTime();
        long difftime = 0;

        //resets the textViews
        didItMove = 0;
        notifyResultCallback(didItMove);


        //Waits 30 seconds by comparing currentdate to the initDate
        //which is initialized right when the service runs

        while(difftime < 30){
            tempDate = Calendar.getInstance().getTime();
            difftime = (tempDate.getTime() - initDate.getTime())/1000;
            System.out.println(difftime);
        }




        running = true;
        Random rand = new Random();
        while (running) {
            // Sleep a tiny bit.
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.getLocalizedMessage();
            }


            SM = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

            //Accelerometer Service

            mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            //Register Sensor Listener

            SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);



        }
    }

    public void addResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Adding result callback");
        resultCallbacks.add(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Removing result callback");
        // We remove the callback...
        resultCallbacks.remove(resultCallback);
        // ...and we clear the list of results.
        // Note that this works because, even though mResultCallbacks is a synchronized set,
        // its cardinality should always be 0 or 1 -- never more than that.
        // We have one viewer only.
        // We clear the buffer, because some result may never be returned to the
        // free buffer, so using a new set upon reattachment is important to avoid
        // leaks.
        freeResults.clear();
    }

    // Creates result bitmaps if they are needed.
    private void createResultsBuffer() {
        // I create some results to talk to the callback, so we can reuse these instead of creating new ones.
        // The list is synchronized, because integers are filled in the service thread,
        // and returned to the free pool from the UI thread.
        freeResults.clear();
        for (int i = 0; i < 10; i++) {
            freeResults.offer(new ServiceResult());
        }
    }

    // This is called by the UI thread to return a result to the free pool.
    public void releaseResult(ServiceResult r) {
        Log.i(LOG_TAG, "Freeing result holder for " + r.intValue);
        freeResults.offer(r);
    }

    public void stopProcessing() {
        running = false;
    }

    public void setTaskState(boolean b) {
        // Do something with b.
    }

    /**
     * Call this function to return the integer i to the activity.
     * @param i
     */
    private void notifyResultCallback(int i) {
        if (!resultCallbacks.isEmpty()) {
            // If we have no free result holders in the buffer, then we need to create them.
            if (freeResults.isEmpty()) {
                createResultsBuffer();
            }
            ServiceResult result = freeResults.poll();
            // If we got a null result, we have no more space in the buffer,
            // and we simply drop the integer, rather than sending it back.
            if (result != null) {
                result.intValue = i;
                for (ResultCallback resultCallback : resultCallbacks) {
                    Log.i(LOG_TAG, "calling resultCallback for " + result.intValue);
                    resultCallback.onResultReady(result);
                }
            }
        }
    }



    /*-----------------------------------------------------------------------------------*/

    //OnSensorChanges checks if there is significant x or y acceleration.
    //if there is a change in x or y greater than 10, then notify 1 to change textViews
    //only when the service is called again is the phone allowed to set didItMove back
    //to 0, so once it is moved then it stays moved.

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        double xVal = sensorEvent.values[0];
        double yVal = sensorEvent.values[1];
        Math.abs(xVal);
        Math.abs(yVal);
        currentDate = Calendar.getInstance().getTime();
        long diffTime = (currentDate.getTime() - initDate.getTime())/1000;


        if(xVal >= 10 || yVal >= 10){
            didItMove = 1;
            if(diffTime >= 30 && didItMove == 1) {
                notifyResultCallback(didItMove);
            }
        }


    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    /*--------------------------------------------------------------------------------------*/


    public interface ResultCallback {
        void onResultReady(ServiceResult result);
    }

}
