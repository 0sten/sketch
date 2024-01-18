/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.sample.ui.test.decoder

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State
import com.github.panpf.assemblyadapter.pager2.AssemblyFragmentStateAdapter
import com.github.panpf.sketch.sample.databinding.FragmentTabPagerBinding
import com.github.panpf.sketch.sample.ui.base.BaseToolbarBindingFragment
import com.github.panpf.sketch.sample.util.repeatCollectWithLifecycle
import com.google.android.material.tabs.TabLayoutMediator

class DecoderTestPagerFragment : BaseToolbarBindingFragment<FragmentTabPagerBinding>() {

    private val viewModel by viewModels<DecoderTestViewModel>()

    override fun onViewCreated(
        toolbar: Toolbar,
        binding: FragmentTabPagerBinding,
        savedInstanceState: Bundle?
    ) {
        toolbar.title = "Decoder"

        viewModel.data.repeatCollectWithLifecycle(viewLifecycleOwner, State.STARTED) {
            binding.pager.adapter = AssemblyFragmentStateAdapter(
                fragment = this,
                itemFactoryList = listOf(DecoderTestImageFragment.ItemFactory()),
                initDataList = List(it.size) { position -> position }
            )

            TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
                tab.text = it[position].name
            }.attach()
        }
    }
}
