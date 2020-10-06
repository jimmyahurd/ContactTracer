package edu.temple.contacttracer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StartupFragment extends Fragment {
    private StartupFragmentListener mListener;

    public StartupFragment() {
        // Required empty public constructor
    }

    public static StartupFragment newInstance() {
        StartupFragment fragment = new StartupFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_startup, container, false);
        view.findViewById(R.id.startServiceButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.startServiceButtonPressed();
            }
        });
        view.findViewById(R.id.stopServiceButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.stopServiceButtonPressed();
            }
        });
        view.findViewById(R.id.positiveTestButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.positiveTestButtonPressed();
            }
        });
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof StartupFragmentListener) {
            mListener = (StartupFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement StartupFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface StartupFragmentListener {
        // TODO: Update argument type and name
        void startServiceButtonPressed();
        void stopServiceButtonPressed();
        void positiveTestButtonPressed();
    }
}
