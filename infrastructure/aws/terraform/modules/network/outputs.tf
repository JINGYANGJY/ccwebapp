output "vpc_id" {
  value = "${aws_vpc.myvpc.id}"
}

output "sb1_id" {
  value = "${aws_subnet.sb1.id}"
}

output "sb2_id" {
  value = "${aws_subnet.sb2.id}"
}

output "sb3_id" {
  value = "${aws_subnet.sb3.id}"
}
