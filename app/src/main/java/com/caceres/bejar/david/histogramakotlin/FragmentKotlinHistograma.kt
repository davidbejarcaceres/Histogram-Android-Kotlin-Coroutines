package com.caceres.bejar.david.histogramakotlin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*
import kotlin.math.PI


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentKotlinHistograma : Fragment() {

    lateinit var btnCalculate: Button
    lateinit var btnCoroutine: Button
    lateinit var txtTime: TextView
    lateinit var txtTimeCoroutine: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return  inflater.inflate(R.layout.fragment_fragment_kotlin_histograma, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnCalculate = view.findViewById(R.id.btnCalculate)
        btnCoroutine = view.findViewById(R.id.btnCorrutine)
        txtTime = view.findViewById(R.id.txtHistogram)
        txtTimeCoroutine = view.findViewById(R.id.txtCorrutine)

        // Calculates the histogram and return the time to show in the textView
        btnCalculate.setOnClickListener {
            txtTime.setText("Calculando...")
            txtTime.setText(calculateHistogram())
        }

        // Calculates in the background using Kotlin Corrutines
        btnCoroutine.setOnClickListener {
            txtTimeCoroutine.setText("Calculando...")
            GlobalScope.launch(Dispatchers.Main) {
                val tiempo = async(Dispatchers.Default) { calculateHistogramCoroutine() } // Get from Default Context
                txtTimeCoroutine.setText("${tiempo.await()} seg.")
            }
        }
    }

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
        return time.toDouble() / 1000.0
    }


    // Not Coroutine Blocking method, uses Main Thread
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
        return time.toDouble() / 1000.0
    }


    // Test Methods to calculate arrays with single and multi-thread solutions
    suspend fun recorreArreglo(): String {

        var timeSingleCore = measureTimeMillis {
            val hSuper = Array(100) { DoubleArray(10000) }
            for (i in 0..9999)
                for (k in 0..99) hSuper[k][i] = (PI * 1000) * (PI / 100) // Inicializa el histograma
        }
        return (timeSingleCore / 1000.0).toString()
    }

    //TODO: Delete later, only demostrates a way to use 3 cores to work with an array
    suspend fun recorreArregloMultiThread(): String = withContext(Dispatchers.Default) {
        // Uses Coroutine to access the array, will use 3 Cores, each one in charge of one third of the array
            val hSuper = Array(100) { DoubleArray(10000) }

            val tiempo1 = async(Dispatchers.Default) {
                val time = measureTimeMillis {
                    for (i in 0..3000)
                        for (k in 0..99) hSuper[k][i] = (PI * 1000) * (PI / 100) // Inicializa el histograma
                }
                time / 1000.0
            }

            val tiempo2 = async(Dispatchers.Default) {
                val time = measureTimeMillis {
                    for (i in 3001..6000)
                        for (k in 0..99) hSuper[k][i] = (PI * 1000) * (PI / 100) // Inicializa el histograma
                }
                time / 1000.0
            }

            val tiempo3 = async(Dispatchers.Default) {
                var time = measureTimeMillis {
                    for (i in 6001..9999)
                        for (k in 0..99) hSuper[k][i] = (PI * 1000) * (PI / 100) // Inicializa el histograma
                }
                time / 1000.0
            }

            var timeMultiCore = tiempo1.await() + tiempo2.await() + tiempo3.await()



        (timeMultiCore).toString()
    }


}
