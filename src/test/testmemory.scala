package tests

import chisel3._
import chisel3.util._
import chisel3.iotesters._

import Common._

class MemoryTests extends SteppedHWIOTester with MemoryOpConstants {
  implicit val sodor_conf = SodorConfiguration()
  val msize = 1 << 21
  val device_under_test = Module( new SyncScratchPadMemory(1, msize) )
  val c = device_under_test
  val req = c.io.core_ports(0).req
  val resp = c.io.core_ports(0).resp
  rnd.setSeed(23L)

  var addr = rnd.nextInt(msize)
  enable_all_debug = true
//  enable_scala_debug = true

  /// INITIALIZE
  poke(req.bits.addr, 0)
  poke(req.bits.data, 0)
  poke(req.bits.fcn, 0)
  poke(req.bits.typ, 0)
  poke(req.valid, 0)

  /// W MT_B
    step(1)
  poke(req.bits.addr, addr)
  poke(req.bits.data, 0x1234)
  poke(req.valid, 1)
  poke(req.bits.fcn, 1) // write
  poke(req.bits.typ, 1) // MT_B
  expect(req.ready, 1)
    step(1)
  expect(resp.valid, 1)
  poke(req.valid, 0)

  /// R MT_W
    step(1)
  poke(req.bits.addr, addr)
  poke(req.valid, 1)
  poke(req.bits.fcn, 0) // read
  poke(req.bits.typ, 3) // MT_W 
  expect(req.ready, 1)
    step(1)
  expect(resp.bits.data, 0x34)
  expect(resp.valid, 1)
  poke(req.valid, 0)

}

class MemoryTester extends ChiselFlatSpec {
  "Debug" should "compile and run without incident" in {
    assertTesterPasses(new MemoryTests, additionalVResources = Seq("/SyncMem.sv") )
  }
}
