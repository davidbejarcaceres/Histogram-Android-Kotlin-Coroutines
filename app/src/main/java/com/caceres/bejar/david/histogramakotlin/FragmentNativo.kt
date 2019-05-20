package com.caceres.bejar.david.histogramakotlin


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentNativo : Fragment() {

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    // Mehtod to access C++ code, is suspend so it can be excecuted in the background
    external suspend fun stringFromJNI(): String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fragment_nativo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = view.findViewById<TextView>(R.id.txtNativoJNI)
        val btnCallNative = view.findViewById<Button>(R.id.btnCallNativeCode)

        btnCallNative.setOnClickListener {
            GlobalScope.launch (Dispatchers.Main) {
                tv.setText("Waiting response from C++ ... ")
                var stringFromC = async(Dispatchers.Default) { stringFromJNI() } // C++ will sleep 3 seconds before returning value
                tv.setText(stringFromC.await())
            }

        }

    }

}
