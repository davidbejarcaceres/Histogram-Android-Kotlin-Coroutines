package com.caceres.bejar.david.histogramakotlin


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.system.measureTimeMillis


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

// TODO: Rename and change types of parameters
private var mParam1: String? = null
private var mParam2: String? = null

lateinit var dim: EditText
lateinit var res: TextView
lateinit var dimen: String
var dimension: Int = 512
lateinit var radioGroup: RadioGroup
var tiempoInicio: Double = 0.0
var tiempoFinal: Double = 0.0

lateinit var A: FloatArray
lateinit var B: FloatArray
lateinit var C: FloatArray


/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentOther : Fragment() {


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
    external fun stringFromJNI(): String
    external fun tiempoC(d: Int): Float
    external fun tiempoCthread(d: Int, A: FloatArray, B: FloatArray, C: FloatArray, modo: Int): Float


    fun  newInstance(param1: String?, param2: String?): FragmentOther{
        var fragment: FragmentOther = FragmentOther()
        val args = Bundle()
        args.putString(ARG_PARAM1, param1)
        args.putString(ARG_PARAM2, param2)
        fragment.arguments = args
        return fragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // get arguments
        mParam1 = arguments?.getString(ARG_PARAM1)
        mParam2 = arguments?.getString(ARG_PARAM2)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fragment_other, container, false)
    }

    fun MultiplicarMatrices() {
        val A = Array(dimension) { FloatArray(dimension) } //Declaro la matriz A
        val B = Array(dimension) { FloatArray(dimension) } //Declaro la matriz B
        val C = Array(dimension) { FloatArray(dimension) } //Declaro la matriz C
        val rand = Random()
        for (i in A.indices) { //Fila matriz A, B y C
            for (j in 0 until A[i].size) { //Columna matriz A, B y C
                A[i][j] = (rand.nextInt(200) / 100.0).toFloat() - 1 //Inicializo las matrices
                B[i][j] = (rand.nextInt(200) / 100.0).toFloat() - 1
                C[i][j] = 0f
            }
        }
        tiempoInicio = System.nanoTime().toDouble()
        for (i in C.indices) { //Filas
            for (j in 0 until C[i].size) { //Columnas
                var cij = C[i][j]
                for (z in 0 until A[i].size) {
                    cij += A[i][z] * B[z][j]
                }
                C[i][j] = cij
            }
        }
        tiempoFinal = System.nanoTime().toDouble()
    }

    fun MultiplicarMatrices1D() {
        tiempoInicio = System.nanoTime().toDouble()
        for (i in 0 until dimension) { //Filas
            for (j in 0 until dimension) { //Columnas
                var cij = C[i * dimension + j]
                for (k in 0 until dimension) {
                    cij += A[i * dimension + k] * B[k * dimension + j]
                }
                C[i * dimension + j] = cij
            }
        }
        tiempoFinal = System.nanoTime().toDouble()
    }

    // Initialize the arrays in background
    suspend fun startArrays(): String{
        A = FloatArray(dimension * dimension)
        B = FloatArray(dimension * dimension)
        C = FloatArray(dimension * dimension)
        val rand = Random()
        for (i in A.indices) { //Array A, B y C
            A[i] = (rand.nextInt(200) / 100.0).toFloat() - 1 //Inicializo las matrices
            B[i] = (rand.nextInt(200) / 100.0).toFloat() - 1
            C[i] = 0f
        }
        return "Ready to start"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val opcJava = view.findViewById(R.id.Java) as RadioButton
        val opcJava1D = view.findViewById(R.id.Java1D) as RadioButton
        val opcNativeC = view.findViewById(R.id.NativeC) as RadioButton
        val opcNativeCpar = view.findViewById(R.id.NativeCpar) as RadioButton
        val opcNativeCneon = view.findViewById(R.id.NativeCneon) as RadioButton
        val opcNativeCtile = view.findViewById(R.id.NativeCtile) as RadioButton

        dim = view.findViewById(R.id.Dimension) as EditText
        val btnCalcula = view.findViewById(R.id.Calcula) as Button
        res = view.findViewById(R.id.ResultadoFragment) as TextView
        radioGroup = view.findViewById(R.id.radiogroup1) as RadioGroup
        radioGroup.clearCheck() // this is so we can start fresh, with no selection on both RadioGroups
        opcJava.setChecked(true) // By default uses the Java one thread option

        GlobalScope.launch(Dispatchers.Main) {
            val arraysStarted = async (Dispatchers.Default) { startArrays() }
            Toast.makeText(context, arraysStarted.await(), Toast.LENGTH_SHORT).show()
        }

        radioGroup?.setOnCheckedChangeListener { group, checkedId ->
            var text = "You selected: ${checkedId}"
        }

        btnCalcula.setOnClickListener {
            res.text = "Calculado..."
            var size = dim.text.toString().toIntOrNull() ?: 512
            dimension = size;

            res.setText("Calculando... ")
            if (opcJava.isChecked) {
                GlobalScope.launch(Dispatchers.Main){
                    res.text = "Calculating..."
                    val timeTotal = measureTimeMillis {
                        var javaTime = async (Dispatchers.Default) { MultiplicarMatrices() }
                        javaTime.await()
                    }
                    res.text = (timeTotal / 1000.00).toString() + "sg"
                }
            } else if (opcJava1D.isChecked) {
                GlobalScope.launch(Dispatchers.Main){
                    res.text = "Calculating..."
                    val timeTotal = measureTimeMillis {
                        var javaTime = async (Dispatchers.Default) { MultiplicarMatrices1D() }
                        javaTime.await()
                    }
                    res.text = (timeTotal / 1000.00).toString() + "sg"
                }
            } else if (opcNativeC.isChecked) {
                GlobalScope.launch(Dispatchers.Main){
                    res.text = "Calculating..."
                    val timeTotal = measureTimeMillis {
                        var timeC = async (Dispatchers.Default) { tiempoC(size).toString()}
                        timeC.await()
                    }
                    res.text = (timeTotal / 1000.00).toString() + "sg"
                }
            } else if (opcNativeCpar.isChecked) {
                GlobalScope.launch(Dispatchers.Main){
                    res.text = "Calculating..."
                    val timeTotal = measureTimeMillis {
                        var timeCThread = async (Dispatchers.Default) { tiempoCthread(size, A, B, C, 2)}
                        timeCThread.await()
                    }
                    res.text = (timeTotal / 1000.00).toString() + "sg"
                }

            } else if (opcNativeCneon.isChecked) {
                if (size and 3 == 0) {
                    GlobalScope.launch(Dispatchers.Main){
                        res.text = "Calculating..."
                        val timeTotal = measureTimeMillis {
                            var timeNeon = async (Dispatchers.Default) { tiempoCthread(size, A, B, C, 3)}
                            timeNeon.await()
                        }
                        res.text = (timeTotal / 1000.00).toString() + "sg"
                    }
                } else
                    res.text = "Not a multiple of 4"
            } else if (opcNativeCtile.isChecked) {
                if (size and 127 == 0) { // multiplo de 128 (tile size=32 * 4 threads) ((dimension & (dimension-1)) ==0 ) // dimension is a power of two
                    GlobalScope.launch(Dispatchers.Main){
                        res.text = "Calculating..."
                        val timeTotal = measureTimeMillis {
                            var timeNeonTile = async (Dispatchers.Default) { tiempoCthread(size, A, B, C, 4)}
                            timeNeonTile.await()
                        }
                        res.text = (timeTotal / 1000.00).toString() + "sg"
                    }
                } else
                    res.text = "Not a multiple of 128"
            }

        }
    }










}
