package com.caceres.bejar.david.histogramakotlin

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;

class MainActivity : AppCompatActivity() {

    val fragNative: FragmentNativo = FragmentNativo()
    val fragKotlinHistogram: FragmentKotlinHistograma = FragmentKotlinHistograma()
    val fragmentOther: FragmentOther = FragmentOther()

    lateinit var fragmentManager: FragmentManager
    lateinit var fragmentTransaction: FragmentTransaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val navBar: BottomNavigationView = findViewById(R.id.nav_bar) // Default Android NavBar
        //configNavBar(navBar)
        configBottomBar()
        this.title = "Calculates Histogram"

        // Loads the first fragment
        loadFragment(fragKotlinHistogram)

    }

    fun loadFragment(fragmentoToLoad: Fragment) {
        // Calls the Fragment Manager to replace the fragment
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction()
        // In case you want to pass some params
        var args: Bundle = Bundle()
        args.putString("param1", "Valor pasado1")
        args.putString("param2", "Valor pasado2")
        fragmentoToLoad.arguments = args

        //Carga el fragmento
        fragmentTransaction.replace(R.id.contenedor_fragment, fragmentoToLoad)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    // COnfigs Bottom NavBar, belongs to external library
    fun configBottomBar() {
        val bottomNavigation = findViewById(R.id.bottom_navigation) as AHBottomNavigation

        // Create items
        val item1 = AHBottomNavigationItem(
            R.string.title_home,
            R.drawable.ic_home_black_24dp,
            R.color.verde
        )
        val item2 =
            AHBottomNavigationItem(
                R.string.title_dashboard,
                R.drawable.ic_dashboard_black_24dp,
                R.color.azul)
        val item3 =
            AHBottomNavigationItem(
                R.string.title_notifications,
                R.drawable.ic_notifications_black_24dp,
                R.color.purpura)

        // Add items
        bottomNavigation.addItem(item1)
        bottomNavigation.addItem(item2)
        bottomNavigation.addItem(item3)

        bottomNavigation.titleState = AHBottomNavigation.TitleState.ALWAYS_SHOW

        // Use colored navigation with circle reveal effect
        bottomNavigation.isColored = true

        // Set listeners for the clicks
        bottomNavigation.setOnTabSelectedListener { position, wasSelected ->
            println(position)
            when (position) {
                0 -> loadFragment(fragKotlinHistogram)
                1 -> loadFragment(fragNative)
                2 -> loadFragment(fragmentOther)
            }
            true
        }
    }

    // Configures the Android default NavBar
    // TODO: Delete later
    private fun configNavBar(navBar: BottomNavigationView) {
        navBar.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(fragKotlinHistogram)
                    this.title = "Histograma Kotlin"
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_dashboard -> {
                    loadFragment(fragNative)
                    this.title = "Message from C++"
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_notifications -> {
                    this.title = "Empty Fragment"
                    loadFragment(fragmentOther)
                    return@OnNavigationItemSelectedListener true
                }
            }
            false

        })
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
