package com.caceres.bejar.david.histogramakotlin


import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.random.Random
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
    external fun mmopenmp(nThreads: Int, sizeMatrix: Int): String
    external suspend fun mmThreads(nThreads: Int, sizeMatrix: Int) : String


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
        val btnOpenMP = view.findViewById<Button>(R.id.mmopenmp)
        val btnThreads = view.findViewById<Button>(R.id.mmThreads)
        val btnKotlin1 = view.findViewById<Button>(R.id.btnKotlin1)


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

        btnOpenMP.setOnClickListener {
            btnOpenMP.setText("Calculation...") // Mutli-Thread: OpenMP C++
            var nThreads: Int = editTxtNumberThread.text.toString().toInt()
            if (nThreads < 1 || nThreads > 10) nThreads = 8 // If number of coroutine is absurd, 8 are used

            GlobalScope.launch(Dispatchers.Main) {
                val tiempommOpenMP = measureTimeMillis {
                    var timeJArray = async(Dispatchers.Default) { mmopenmp( nThreads, 1000 ) } // Get from Default Context
                    println("END FROM C++ mm OpenMP ${timeJArray.await().toString()}")
                }
                println("END FROM C++ using Array not vectors ${tiempommOpenMP.toString()}")
                btnOpenMP.setText("${tiempommOpenMP / 1000.00} seg.")
            }
        }


        btnThreads.setOnClickListener {
            btnThreads.setText("Calculation...") // Mutli-Thread: OpenMP C++
            var nThreads: Int = editTxtNumberThread.text.toString().toInt()
            if (nThreads < 1 || nThreads > 10) nThreads =
                8 // If number of coroutine is absurd, 8 are used

            GlobalScope.launch(Dispatchers.Main) {
                val tiempoThreads = measureTimeMillis {
                    var timeMMThreads = async (Dispatchers.Default) {mmThreads(nThreads, 1000)} // Get from Default Context
                    println("END FROM C++ mm Threads ${timeMMThreads.await().toString()}")
                }
                println("END FROM C++ using Manual Threads ${tiempoThreads.toString()}")
                btnThreads.setText("${tiempoThreads / 1000.00} seg.")
            }
        }

        btnKotlin1.setOnClickListener {
            btnKotlin1.setText("Calculation...") // Mutli-Thread: OpenMP C++
            var nThreads: Int = editTxtNumberThread.text.toString().toInt()
            if (nThreads < 1 || nThreads > 10) nThreads = 8 // If number of coroutine is absurd, 8 are use


            var listaJobsInit: ArrayList<Deferred<Long>> = arrayListOf<Deferred<Long>>()
            GlobalScope.launch(Dispatchers.Main) {
                val tiempommKotlin = measureTimeMillis {
                    for (i in 0 until nThreads){
                        listaJobsInit.add( async (Dispatchers.Default) { matrixMultiplyKotlin(i, nThreads) } ) // Get from Default Context)
                    }
                    // waits for all jobs to be done.
                    var sumEachThread = listaJobsInit.awaitAll()
                }


                println("END FROM Kotlin with Coroutines ${tiempommKotlin.toString()}")
                btnKotlin1.setText("${tiempommKotlin / 1000.00} seg.")
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


    suspend fun matrixMultiplyKotlin(taskId: Int, nTasks: Int): Long  {
        var max_RANDOM: Int = 2147483647
        var N: Int = 1000
        val matrixa = Array(N) { FloatArray(N) }
        val matrixb = Array(N) { FloatArray(N) }
        val matrixc = Array(N) { FloatArray(N) }
        var ini = (  (taskId * N) / nTasks).toInt()
        var fin = (((taskId +1).toLong() * N) / nTasks).toInt()


        for ( i in ini until fin){
            for (j in 0 until N){
                matrixa[i][j] = Random.nextInt(0, max_RANDOM) * 1.1f;
                matrixb[i][j] = Random.nextInt(0, max_RANDOM) * 1.1f;
            }
        }

        val tiempoKotlin = measureTimeMillis {
            for ( i in ini until fin){
                for (j in 0 until N){
                    for (k in 0 until N){
                        matrixc[i][j] += matrixa[i][k] * matrixb[k][j];
                    }
                }
            }
        }
        print("Tiempo con Kotlin = "+ tiempoKotlin + "\n")
        return tiempoKotlin / 100;
    }

}
