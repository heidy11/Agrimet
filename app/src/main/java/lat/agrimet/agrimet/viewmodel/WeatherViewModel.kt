package lat.agrimet.agrimet.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WeatherViewModel : ViewModel() {
    val temperature = MutableLiveData<String>()
    val humidity    = MutableLiveData<String>()
    val wind        = MutableLiveData<String>()
    val uvIndex     = MutableLiveData<String>()
}