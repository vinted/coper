package com.vinted.coper.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.vinted.coper.example.databinding.FragmentExampleSelectBinding

class ExampleSelectFragment : Fragment() {

    private val viewBinding: FragmentExampleSelectBinding by viewBinding(
        FragmentExampleSelectBinding::bind
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_example_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.exampleSelectRequestViewmodel.setOnClickListener {
            (requireActivity() as MainActivity).addFragmentToActivity(
                PermissionExampleInBusinessSideFragment()
            )
        }
        viewBinding.exampleSelectRequestFragment.setOnClickListener {
            (requireActivity() as MainActivity).addFragmentToActivity(PermissionExampleInUIFragment())
        }
    }
}
