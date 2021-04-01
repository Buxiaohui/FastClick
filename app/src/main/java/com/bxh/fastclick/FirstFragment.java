package com.bxh.fastclick;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.buxiaohui.fastclick.FC;
import com.bxh.fastclick.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {
   private static final String TAG = "FirstFragment";
    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @FC
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the first btn");
            }
        });
        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @FC(timeInterval = 1000)
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the second btn");
            }
        });
        binding.buttonThird.setOnClickListener(new View.OnClickListener() {
            @FC(tag = "group-0")
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the third btn");
            }
        });
        binding.buttonFourth.setOnClickListener(new View.OnClickListener() {
            @FC(timeInterval = 1000, tag = "group-0")
            @Override
            public void onClick(View view) {
                Log.e(TAG,"click the fourth btn");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}