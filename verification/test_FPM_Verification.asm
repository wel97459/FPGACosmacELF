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
	db load_d
	dbrevstr "+1337000-06"
	
	sep r4
	db fpm_D2AC
	
	sep r4
	db fpm_AC2D
	
	sep r4
	db D2String
	db dtest

db 00 --halt simulation and emulator
	
	sep r4
	db puts
	db dteststring

loop:
    br low loop

space 0100
load FPM.bin

dteststring:
dbstr "D: "
dtest:
db 00 00 00 00 00 00 00 00 00 00 00 
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

D2String:
	lda r6
	phi rb
	lda r6
	plo rb
	
	ldi 01
	phi ra
	ldi 14
	plo ra
	sex ra
	
	ldxa
	str rb
	inc rb
	
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
	
	inc rb
	ldxa
	ani 0f
	adi 30
	str rb
sep r5 

load_d:
	ldi 01
	phi ra
	ldi 1e
	plo ra
	sex ra
	
	lda r6 --de low
	stxd
	lda r6 --de high
	stxd
	lda r6 --de sign
	stxd

	lda r6 --de mantissa b6
	stxd
	lda r6 --de mantissa b5
	stxd
	lda r6 --de mantissa b4
	stxd
	lda r6 --de mantissa b3
	stxd
	lda r6 --de mantissa b2
	stxd
	lda r6 --de mantissa b1
	stxd
	lda r6 --de mantissa b0
	stxd
	lda r6 --de mantissa sign
	stxd
sep r5