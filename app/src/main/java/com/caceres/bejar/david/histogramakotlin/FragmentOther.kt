package com.caceres.bejar.david.histogramakotlin


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

// TODO: Rename and change types of parameters
private var mParam1: String? = null
private var mParam2: String? = null

/**
 * A simple [Fragment] subclass.
 *
 */
class FragmentOther : Fragment() {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


}
