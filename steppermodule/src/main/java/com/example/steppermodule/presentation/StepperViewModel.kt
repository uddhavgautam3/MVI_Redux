package com.example.steppermodule.presentation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.example.steppermodule.domain.StepperService
import com.example.steppermodule.domain.StepperServiceImpl
import com.example.steppermodule.presentation.StepperView.State
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject


@HiltViewModel
class StepperViewModel @Inject constructor(
    @ApplicationContext application: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val service = StepperServiceImpl()
    val serviceState: ObservableTransformer<StepperService.Action, StepperService.Result>
        get() = service.state()

    //below two lines should be in order
    val defaultViewState: State = State.Loaded(25)
    private val statesObservable: Observable<State> by lazy { composeFlow() }

    //LiveDataReactiveStreams.fromPublisher(publisher) changed to publisher.toLiveData() in androidx.lifecycle:lifecycle-runtime-ktx:2.6.1
    private val stateFlowable: Flowable<State> = statesObservable.toFlowable(
        BackpressureStrategy.BUFFER
    )

    fun state(): LiveData<State> = stateFlowable.toLiveData()

    private val reducer: BiFunction<State, State, State>
        get() = BiFunction { _: State, result: State ->
            val transition = when (result) {
                is State.Loading -> {
                    State.Loading
                }
                is State.AfterLoading -> {
                    State.AfterLoading
                }
                is State.Error -> {
                    State.Error("Error Occurred!")
                }
                is State.Loaded -> {
                    State.Loaded(5)
                }
            }
            transition
        }

    private fun composeFlow(): Observable<State> {
        val intentsSubject: PublishSubject<State> = PublishSubject.create()

        return intentsSubject
            .scan(defaultViewState, reducer)
            .distinctUntilChanged()
            .replay(1)
            .autoConnect(0)
    }

}