package edu.temple.contacttracer;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

//Fragment used to hold the DatePickerDialog
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    DateSelectedListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Calendar calendar = Calendar.getInstance();
        return new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    //When user selects a date, it passes to the Activity it is contained in the date selected
    //in milliseconds
    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        LocalDateTime date = LocalDateTime.of(year, month, dayOfMonth, 0, 0);
        ZoneOffset offset = ZoneId.of("America/New_York").getRules().getOffset(date);
        mListener.dateSelected(date.toEpochSecond(offset));
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DateSelectedListener) {
            mListener = (DateSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DateSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface DateSelectedListener{
        void dateSelected(long time);
    }
}
