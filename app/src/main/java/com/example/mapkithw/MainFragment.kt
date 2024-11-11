package com.example.mapkithw

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mapkithw.databinding.FragmentMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMainBinding.inflate(inflater,container,false)



        val permissionLauncherLocale = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("@@@", "GPS разрешение дано")
            } else {
                Log.d("@@@", "GPS разрешение не дано")
            }
        }

        permissionLauncherLocale.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        val animationImage = AnimationUtils.loadAnimation(requireContext(),R.anim.fade)
        binding.mainFragmentImageView.startAnimation(animationImage)

        val animationText = AnimationUtils.loadAnimation(requireContext(),R.anim.fade)
        binding.mainFragmentTextView.startAnimation(animationText)

        lifecycleScope.launch { toMapFragment() }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun toMapFragment(){
        delay(5000L)
        findNavController().navigate(R.id.action_mainFragment_to_mapFragmentFragment)

    }

}