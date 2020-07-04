package `in`.thenvn.artista

import `in`.thenvn.artista.databinding.ActivityMainBinding
import `in`.thenvn.artista.media.MediaItemsAdapter
import `in`.thenvn.artista.media.MediaViewModel
import `in`.thenvn.artista.media.MediaViewModelFactory
import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

private const val RC_PERMISSIONS: Int = 2

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val TAG: String = MainActivity::class.java.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        val viewModelFactory =
            MediaViewModelFactory(application)
        val mediaViewModel =
            ViewModelProvider(this, viewModelFactory).get(MediaViewModel::class.java)

        binding.mediaViewModel = mediaViewModel
        val adapter =
            MediaItemsAdapter(MediaItemsAdapter.MediaItemClickListener { uri ->
                Log.d(TAG, "Item clicked with uri $uri")
//            mediaViewModel.onMediaItemClicked(uri)
            })

        adapter.submitList(mediaViewModel.mediaItemListLiveData?.value)


        val layoutManager = GridLayoutManager(this, 3)
        binding.mediaGrid.layoutManager = layoutManager
        binding.mediaGrid.setHasFixedSize(true)
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_space)
        binding.mediaGrid.addItemDecoration(SpaceItemDecoration(spacing, 3))

        binding.mediaGrid.adapter = adapter
        binding.lifecycleOwner = this

        mediaViewModel.mediaItemListLiveData?.observe(this, Observer { mediaUriList ->
            Log.d(TAG, "Fetched media list!")
            binding.mediaGrid.adapter = adapter
            adapter.submitList(mediaUriList)
        })

        if (EasyPermissions.hasPermissions(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {

        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(RC_PERMISSIONS)
    private fun requestPermissions() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (EasyPermissions.hasPermissions(this, *permissions)) {
            Log.d(TAG, "Permissions already granted.")
        } else {
            EasyPermissions
                .requestPermissions(PermissionRequest.Builder(this, RC_PERMISSIONS, *permissions)
                    .setRationale("Required permissions")
                    .setPositiveButtonText("GRANT")
                    .setNegativeButtonText("DENY")
                    .build())
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "Permission is granted")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            Log.d(TAG, "Is permission grated?")
        }
    }

}