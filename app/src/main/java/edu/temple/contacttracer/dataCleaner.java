package edu.temple.contacttracer;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Set;

public class dataCleaner extends Worker {
    public dataCleaner(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        UUIDtracker data = (UUIDtracker)getApplicationContext();
        data.cleanData();
        return Result.success();
    }
}
