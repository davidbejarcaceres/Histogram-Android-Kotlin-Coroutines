package com.caceres.bejar.david.histogramakotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.log10


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentKotlinHistograma : Fragment() {

    lateinit var btnSession2MultiThread: Button
    lateinit var txtSession2MultiThread: TextView

    lateinit var checkBoxMultiThread: CheckBox
    lateinit var editTxtNumberThread: EditText



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return  inflater.inflate(R.layout.fragment_fragment_kotlin_histograma, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        btnSession2MultiThread = view.findViewById<Button>(R.id.btnMultiThread)
        txtSession2MultiThread = view.findViewById(R.id.txtMultiThead)

        checkBoxMultiThread = view.findViewById(R.id.checkBMultiThread)
        editTxtNumberThread = view.findViewById(R.id.txtCoroutinesNumber)



        checkBoxMultiThread.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                editTxtNumberThread.setEnabled(true);
            } else{
                editTxtNumberThread.setEnabled(false);
            }
        });


        // Calculates histogram with multiThread capabilities, SESSION 2
        btnSession2MultiThread.setOnClickListener {
            txtSession2MultiThread.setText("Calculando...")
            if (checkBoxMultiThread.isChecked){
                GlobalScope.launch(Dispatchers.Main) {
                    var coroutines: Int = editTxtNumberThread.text.toString().toInt()
                    if (coroutines < 1 || coroutines > 16) coroutines = 8 // If number of coroutine is absurd, 8 are used
                    var listaJobsInit: ArrayList<Deferred<Double>> = arrayListOf<Deferred<Double>>()

                    var time = measureTimeMillis {
                        for (i in 0 until coroutines){
                            listaJobsInit.add(async(Dispatchers.Default) { histrogramSession2MultiThread(i, coroutines) }) // Get from Default Context)
                        }
                        // waits for all jobs to be done.
                        var sumEachThread = listaJobsInit.awaitAll()
                    }
                    txtSession2MultiThread.setText("${time / 1000.00} seg.")
                    println("TOTAL TIME ONE THREAD: ${time / 1000.00} seg")
                }
            } else{
                GlobalScope.launch(Dispatchers.Main) {
                    var tiempoTotal = measureTimeMillis {
                        val tiempo1 = async(Dispatchers.Default) { histrogramSession2MultiThread(0, 1) } // Get from Default Context
                        txtSession2MultiThread.setText("${tiempo1.await()} seg.")
                    }
                    txtSession2MultiThread.setText("${tiempoTotal / 1000.00} seg.")
                    println("TOTAL TIME ONE THREAD: ${tiempoTotal / 1000.00} seg")
                }
            }
        }

    }//END OnViewCreated



    // Suspend method to use with Coroutine
    suspend fun calculateHistogramCoroutine(): String = withContext(Dispatchers.Default) {
        val h = Array(3) { ShortArray(256) } //array con el histograma
        // Ejemplo de la llamada a un método nativo
        val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
        val imagen = Array(3) { Array(tam){ShortArray(tam)} }
        val tiempo: Double
        for (i in 0..255)
            for (k in 0..2) h[k][i] = 0 // Inicializa el histograma
        for (i in 0 until tam)
            for (j in 0 until tam)
                for (k in 0..2)
                    imagen[k][i][j] = ((i * j) % 256).toShort() // Inicializa la imagen
        tiempo = histograma(tam, imagen, h)
        val stringtiempo = tiempo.toString() //Paso el resultado a string
        stringtiempo
    }

    fun histogramaCoroutine(tam: Int, imagen: Array<Array<ShortArray>>, h: Array<ShortArray>): Double {
        val time = measureTimeMillis {
            for (i in 0 until tam)
                for (j in 0 until tam)
                    for (k in 0..2)
                        h[k][imagen[k][i][j].toInt()]++ // Contabiliza el número de veces que aparece cada valor.
        }
        return time.toDouble() / 100.0
    }

    // Not Coroutine, Blocking method, uses Main Thread
    fun calculateHistogram(): String{
        val h = Array(3) { ShortArray(256) } //array con el histograma
        // Ejemplo de la llamada a un método nativo
        val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
        val imagen = Array(3) { Array(tam){ShortArray(tam)} }
        val tiempo: Double
        for (i in 0..255)
            for (k in 0..2) h[k][i] = 0 // Inicializa el histograma
        for (i in 0 until tam)
            for (j in 0 until tam)
                for (k in 0..2)
                    imagen[k][i][j] = ((i * j) % 256).toShort() // Inicializa la imagen
        tiempo = histograma(tam, imagen, h)
        val stringtiempo = tiempo.toString() //Paso el resultado a string
        return stringtiempo
    }

    fun histograma(tam: Int, imagen: Array<Array<ShortArray>>, h: Array<ShortArray>): Double {
        val time = measureTimeMillis {
            for (i in 0 until tam)
                for (j in 0 until tam)
                    for (k in 0..2)
                        h[k][imagen[k][i][j].toInt()]++ // Contabiliza el número de veces que aparece cada valor.
        }
        return time.toDouble() / 100.0
    }

    suspend fun histrogramSession2(): Double = withContext(Dispatchers.Default){
        var time = measureTimeMillis {
            var aux: Long = 0
            val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
            val imagen = Array(3) { Array(tam){IntArray(tam)} }
            val h = Array(3) { IntArray(256) } //array con el histograma
            for (i in 0..255)
            // Inicializa el histograma.
                for (k in 0..2) h[k][i] = 0
            for (i in 0 until tam)
            // Inicializa la imagen.
                for (j in 0 until tam)
                    for (k in 0..2) imagen[k][i][j] = (i * j % 256)
            for (i in 0 until tam)
            // Contabiliza el nº veces que aparece cada valor.
                for (j in 0 until tam)
                    for (k in 0..2) h[k][imagen[k][i][j]]++

            for (i in 0 until tam) { // Modificar imagen utilizando el histograma
                for (j in 0 until tam)
                    for (k in 0..2) {
                        for (x in 0..255) {
                            aux = (h[k][x] * h[k][x] * h[k][x] * h[k][x] - h[k][x] * h[k][x] * h[k][x] + h[k][x] * h[k][x]).toLong()
                            h[k][x] = (aux % 256).toInt()
                        }
                        imagen[k][i][j] = (imagen[k][i][j] * h[k][imagen[k][i][j]])
                    }
            }
        }
        //Returns the time used for the operation
        time.toDouble()
        val totalTime = time.toDouble() / 1000.00
        totalTime
    }

    suspend fun histrogramSession2MultiThread(taskId: Int, nTasks: Int): Double = withContext(Dispatchers.Default){
        var time = measureTimeMillis {
            var aux: Long = 0
            val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
            val imagen = Array(3) { Array(tam){IntArray(tam)} }
            val h = Array(3) { IntArray(256) } //array con el histograma
            var ini = (  (taskId * tam) / nTasks).toInt()
            var fin = (((taskId +1).toLong() * tam) / nTasks).toInt()

            for (i in 0..255)
            // Inicializa el histograma.
                for (k in 0..2) h[k][i] = 0
            for (i in 0 until tam)
            // Inicializa la imagen.
                for (j in 0 until tam)
                    for (k in 0..2) imagen[k][i][j] = (i * j % 256)
            for (i in 0 until tam)
            // Contabiliza el nº veces que aparece cada valor.
                for (j in 0 until tam)
                    for (k in 0..2) h[k][imagen[k][i][j]]++

            for (i in ini until fin) { // Modificar imagen utilizando el histograma
                for (j in 0 until tam)
                    for (k in 0..2) {
                        for (x in 0..255) {
                            aux = (h[k][x] * h[k][x] * h[k][x] * h[k][x] - h[k][x] * h[k][x] * h[k][x] + h[k][x] * h[k][x]).toLong()
                            h[k][x] = (aux % 256).toInt()
                        }
                        imagen[k][i][j] = (imagen[k][i][j] * h[k][imagen[k][i][j]])
                    }
            }
        }
        //Returns the time used for the operation
        time.toDouble()
        val totalTime = time.toDouble() / 1000.00
        totalTime
    }

    suspend fun histrogramMultiOptimized(coroutinesN: Int): Double = withContext(Dispatchers.Default){
        var time = measureTimeMillis {
            var aux: Long = 0
            val tam = 1000 // Imagen de 1.000 x 1.000 pixeles RGB
            val imagen = Array(3) { Array(tam){IntArray(tam)} }
            val h = Array(3) { IntArray(256) } //array con el histograma

            for (i in 0..255)
            // Inicializa el histograma.
                for (k in 0..2) h[k][i] = 0
            for (i in 0 until tam)
            // Inicializa la imagen.
                for (j in 0 until tam)
                    for (k in 0..2) imagen[k][i][j] = (i * j % 256)
            for (i in 0 until tam)
            // Contabiliza el nº veces que aparece cada valor.
                for (j in 0 until tam)
                    for (k in 0..2) h[k][imagen[k][i][j]]++

            // Modificar imagen utilizando el histograma
            var listaJobsInit: ArrayList<Deferred<Double>> = arrayListOf<Deferred<Double>>()
            for (i in 0 until coroutinesN){
                listaJobsInit.add(async(Dispatchers.Default) { histogramaMultiThread(tam, imagen, h, i, coroutinesN) }) // Get from Default Context)
            }
            // waits for all jobs to be done.
            var sumEachThread = listaJobsInit.awaitAll()
        }
        //Returns the time used for the operation
        time.toDouble()
        val totalTime = time.toDouble() / 1000.00
        totalTime
    }

    suspend fun histogramaMultiThread(tam: Int, imagen: Array<Array<IntArray>>, h: Array<IntArray>, taskId: Int, nTasks: Int): Double {
        val time = measureTimeMillis {
            var aux: Long = 0
            var ini = (  (taskId * tam) / nTasks).toInt()
            var fin = (((taskId +1).toLong() * tam) / nTasks).toInt()

            for (i in ini until fin) { // Modificar imagen utilizando el histograma
                for (j in 0 until tam)
                    for (k in 0..2) {
                        for (x in 0..255) {
                            aux = (h[k][x] * h[k][x] * h[k][x] * h[k][x] - h[k][x] * h[k][x] * h[k][x] + h[k][x] * h[k][x]).toLong()
                            h[k][x] = (aux % 256).toInt()
                        }
                        imagen[k][i][j] = (imagen[k][i][j] * h[k][imagen[k][i][j]])
                    }
            }
        }
        return time.toDouble() / 1000.0
    }
}
