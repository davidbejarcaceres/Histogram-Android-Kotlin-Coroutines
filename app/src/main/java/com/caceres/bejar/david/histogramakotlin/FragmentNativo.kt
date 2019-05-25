package com.caceres.bejar.david.histogramakotlin


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_fragment_nativo.*
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
    external suspend fun stringFromJNI(): String
    external suspend fun calcHistogramC(tam: Int, imagen: ShortArray, h: IntArray): String
    external suspend fun histogramaCpth(tam: Int, nthreads: Int, imagen: ShortArray, h: IntArray): String
    external fun histogramaOpenMP(tam: Int, imagen: ShortArray, h: IntArray): String

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
        val btnNativeHist = view.findViewById<Button>(R.id.btnCallNativeCode)
        val btnNativeOpenMP = view.findViewById<Button>(R.id.btnNativeHistogram)
        val txtTimeNative = view.findViewById<TextView>(R.id.txtHistogramNative)
        val checkMultiThread = view.findViewById<CheckBox>(R.id.checkBMulti)

        btnNativeHist.setOnClickListener {
            if (!checkMultiThread.isChecked){
                GlobalScope.launch (Dispatchers.Main) {
                    tv.setText("Calculationg with C++ ... ")
                    var time = measureTimeMillis {
                        val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
                        val hvector = IntArray(3 * 256) //vector con el histograma
                        val imagenvector = ShortArray(3 * tam * tam)

                        var singleThreadJob = async(Dispatchers.Default) { calcHistogramC(tam, imagenvector, hvector) }
                        println("END FROM C++ Method ${singleThreadJob.await()}")
                    }
                    tv.setText(time.toString())
                }
            } else {
                GlobalScope.launch (Dispatchers.Main) {
                    tv.setText("Calculating... ")
                    var time = measureTimeMillis {
                        val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
                        val hvector = IntArray(3 * 256) //vector con el histograma
                        val imagenvector = ShortArray(3 * tam * tam)

                        var MultiThreadJob = async(Dispatchers.Default) { histogramaCpth(tam, 8, imagenvector, hvector) }
                        println("END FROM C++ Method ${MultiThreadJob.await().toString()}")
                    }
                    tv.setText(time.toString())
                }
            }
        }


        btnNativeOpenMP.setOnClickListener {
            GlobalScope.launch (Dispatchers.Main) {
                txtTimeNative.setText("Calculatng... ")
                var time = measureTimeMillis {
                    val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
                    val hvector = IntArray(3 * 256) //vector con el histograma
                    val imagenvector = ShortArray(3 * tam * tam)

                    var MultiThreadJob = async(Dispatchers.Default) { histogramaOpenMP(tam, imagenvector, hvector) }
                    println("END FROM C++ Method ${MultiThreadJob.await().toString()}")
                }
                txtTimeNative.setText(time.toString())
            }

        }




    }

}
