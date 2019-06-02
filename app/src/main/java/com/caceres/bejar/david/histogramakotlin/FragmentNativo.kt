package com.caceres.bejar.david.histogramakotlin


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis


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
    external  fun stringFromJNI(): String
    external fun histogramCSingleTh(): Int
    external fun histogramCMultiOpenMPBest(nThreads: Int): Int

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fragment_nativo, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val checkMultiThread = view.findViewById<CheckBox>(R.id.checkBMulti)
        val btnJArrayNative = view.findViewById<Button>(R.id.btnJArray)
        val txtJArray = view.findViewById<TextView>(R.id.txtJArray)
        val editTxtNumberThread = view.findViewById<TextView>(R.id.txtThreadsNumber)

        btnJArrayNative.setOnClickListener {
            if (!checkMultiThread.isChecked){ // Single-Thread: C++
                txtJArray.setText("Calculation...")
                GlobalScope.launch(Dispatchers.Main) {
                    val tiempoCArray = measureTimeMillis {
                        var timeJArray = async(Dispatchers.Default) { histogramCSingleTh() } // Get from Default Context
                        println("END FROM C++ using Array not vectors ${timeJArray.await().toString()}")
                    }
                    println("END FROM C++ using Array not vectors ${tiempoCArray.toString()}")
                    txtJArray.setText("${tiempoCArray.toString()}")
                }
            } else {
                var nThreads: Int = editTxtNumberThread.text.toString().toInt()
                if (nThreads < 1 || nThreads > 10) nThreads = 8 // If number of coroutine is absurd, 8 are used
                txtJArray.setText("Calculation...") // Mutli-Thread: OpenMP C++
                GlobalScope.launch(Dispatchers.Main) {
                    val tiempoCArray = measureTimeMillis {
                        var timeJArray = async(Dispatchers.Default) { histogramCMultiOpenMPBest( nThreads ) } // Get from Default Context
                        println("END FROM C++ using Array not vectors ${timeJArray.await().toString()}")
                    }
                    println("END FROM C++ using Array not vectors ${tiempoCArray.toString()}")
                    txtJArray.setText("${tiempoCArray.toString()}")
                }
            }
        }


        checkMultiThread.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                editTxtNumberThread.setEnabled(true);
            } else{
                editTxtNumberThread.setEnabled(false);
            }
        });
    }

}
