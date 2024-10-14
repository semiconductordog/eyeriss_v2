package eyeriss.utils

import chisel3._
import chisel3.experimental._
import chisel3.util._

class ScanChain(len: Int) extends Module {
    val io = IO(new Bundle{
        val in = Input(Bool())
        val en = Input(Bool())

        val out = Output(UInt(len.W))
    })

    val ChainD = Wire(UInt(len.W))
    val Chain = RegEnable(ChainD, 0.U, io.en)
    ChainD := Cat(io.in, Chain(len-1, 0)) >> 1

    io.out := Chain
    
} // ScanChain

object ScanChain{
    def apply(in: Bool, en: Bool, len: Int) = {
        val u = Module(new ScanChain(len))
        u.io.in := in
        u.io.en := en
        u.io.out
    }
}

