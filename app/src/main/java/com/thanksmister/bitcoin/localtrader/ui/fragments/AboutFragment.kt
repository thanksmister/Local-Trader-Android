/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.ui.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.constants.Constants
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment
import kotlinx.android.synthetic.main.view_about.*

import timber.log.Timber

class AboutFragment : BaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_about, container, false)
    }

    override fun onViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(fragmentView, savedInstanceState)
        guidesButton.setOnClickListener {
            guides()
        }
        sendAccountButton.setOnClickListener {
            support()
        }
        projectButton.setOnClickListener {
            gitHub()
        }
        licenseButton.setOnClickListener {
            showLicense()
        }
        rateApplicationButton.setOnClickListener {
            rate()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {
            if (activity != null) {
                val packageInfo = requireActivity().packageManager.getPackageInfo(activity!!.packageName, 0)
                val versionText = " v" + packageInfo.versionName
                val versionName = requireActivity().findViewById<View>(R.id.versionName) as TextView
                versionName.text = versionText
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e.message)
        }
    }

    private fun rate() {
        if (isAdded && activity != null) {
            val appName = Constants.GOOGLE_PLAY_RATING
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appName")))
            } catch (ex: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=$appName")))
            }
        }
    }

    private fun gitHub() {
        if (isAdded && activity != null) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GITHUB)))
            } catch (ex: android.content.ActivityNotFoundException) {
                Toast.makeText(requireActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guides() {
        if (isAdded && activity != null) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GUIDES_URL)))
            } catch (ex: android.content.ActivityNotFoundException) {
                Toast.makeText(requireActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun support() {
        if (isAdded && activity != null) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.SUPPORT_URL)))
            } catch (ex: android.content.ActivityNotFoundException) {
                Toast.makeText(requireActivity(), getString(R.string.toast_error_no_installed_ativity), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLicense() {
        if (isAdded && activity != null) {
            dialogUtils.showAlertHtmlDialog(requireActivity(), getString(R.string.license))
        }
    }

    companion object {
        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}