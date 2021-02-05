package com.vinted.coper.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_example_select.*

class ExampleSelectFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_example_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        example_select_request_viewmodel.setOnClickListener {
            (requireActivity() as MainActivity).addFragmentToActivity(
                PermissionExampleInBusinessSideFragment()
            )
        }
        example_select_request_fragment.setOnClickListener {
            (requireActivity() as MainActivity).addFragmentToActivity(PermissionExampleInUIFragment())
        }
    }
}
