package org.acmvit.gitpositive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.acmvit.gitpositive.local.User
import org.acmvit.gitpositive.local.UserDao
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiInterface: ApiInterface,
    private val userDao: UserDao
) : ViewModel() {

    private val _viewState = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = _viewState

    fun getUserData(username: String) {
        _viewState.value = ViewState.Loading
        apiInterface.getData(username).enqueue(object : Callback<UserData?> {
            override fun onResponse(call: Call<UserData?>, response: Response<UserData?>) {
                if (response.isSuccessful && response.body() != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        userDao.insertUser(User(response.body()!!.id, username))
                    }
                    _viewState.value = ViewState.Success(response.body()!!)
                } else {
                    _viewState.value = ViewState.Error(
                        JSONObject(
                            response.errorBody()?.string().orEmpty()
                        ).get("message") as String
                    )
                }
            }

            override fun onFailure(call: Call<UserData?>, t: Throwable) {
                _viewState.value = ViewState.Error(t.message.orEmpty())
            }
        })
    }


    sealed class ViewState {
        object Loading : ViewState()
        data class Error(val message: String) : ViewState()
        data class Success(val userData: UserData) : ViewState()
    }
}