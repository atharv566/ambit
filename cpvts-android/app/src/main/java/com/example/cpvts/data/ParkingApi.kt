package com.example.cpvts.data

import retrofit2.Response
import retrofit2.http.*

interface ParkingApi {
    @GET("health") suspend fun health(): Response<Envelope<Health>>
    @GET("dashboard") suspend fun dashboard(): Response<Envelope<Snapshot>>
    @GET("slots") suspend fun slots(): Response<Envelope<List<Slot>>>
    @POST("check-ins") suspend fun checkIn(@Body request: CheckInRequest): Response<Envelope<ParkingRecord>>
    @POST("check-outs") suspend fun checkOut(@Body request: CheckOutRequest): Response<Envelope<ParkingRecord>>
    @GET("vehicles/{vehicleNumber}") suspend fun find(@Path("vehicleNumber") vehicleNumber: String): Response<Envelope<ParkingRecord>>
    @GET("summary") suspend fun summary(@Query("date") date: String? = null): Response<Envelope<Snapshot>>
}
