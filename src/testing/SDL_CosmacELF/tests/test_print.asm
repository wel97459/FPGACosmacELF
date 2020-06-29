?Def=asm1802
#define fpm_add 0400
#define fpm_sub 029e
#define fpm_mult 02b7
#define fpm_div 034c
#define fpm_02AC 03c3
#define fpm_02OP 03d9
#define fpm_sqroot 0773
#define fpm_D2AC 0500
#define fpm_AC2D 0598
#define fpm_V2VC 06da
#define fpm_V2OP 0705
#define fpm_AC2V 072c
#define fpm_load 0754

init:
#define ramEnd 00ff --stack pointer start
--setup the new progam counter
    ghi r0
    phi r3
	phi r6
    ldi low main
    plo r3
	plo r6
	
--setup call
    ghi r0
    phi r4
    ldi low call
    plo r4

--setup return
    ghi r0
    phi r5
    ldi low return
    plo r5

--setup stack
    ldi high ramEnd
    phi r2
    ldi low ramEnd
    plo r2

-----------------------Start of Standard Call and Return Technique-------------------------
exita:
    sep r3

call:
--set x to stack
    sex r2

--save r6 to stack
    ghi r6
    stxd
    glo r6
    stxd

--move r3 into r6, to save return address
    ghi r3
    phi r6
    glo r3
    plo r6

--load the address of subroutine
    lda r6
    phi r3
    lda r6
    plo r3

--exit and run subroutine
    br low exita

exitr:
    sep r3

return:
--set x to stack
    sex r2
--copy r6 into r3
    ghi r6
    phi r3
    glo r6
    plo r3
--increment stack pointer
    inc r2
--restore pointer from stack
    ldxa
    plo r6
    ldx
    phi r6
--head back to R3 progam counter
    br low exitr
-----------------------End of Standard Call and Return Technique-------------------------

main:
	
	sep r4
	db puts
	db dteststring

loop:
    br low main

space 0100

dteststring:
dbstrne "D: "
dtest:
--db 00 00 00 00 00 00 00 00 00 00 00 
dbstrne "Winston!"
db 0a 0d 00

puts:
	lda r6
    phi ra
    lda r6
    plo ra
	sex ra
	
puts_full:
	bn1 low puts_full
	out n1
	ldx
	bnz low puts_full
	
sep r5