package eyeriss.noc.router

import chisel3._
import chisel3.experimental._
import chisel3.util._
import eyeriss.noc._

class IActRouter extends Module {
    // val io = IO(new Bundle{
        
    // })

    val I = IO(Flipped(Vec(4, new IActRouterPort)))
    val O = IO(Vec(4, new IActRouterPort))

} // IActRouter

