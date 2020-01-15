----------------------------------------------------------------------------------
-- Company:
-- Engineer:
--
-- Create Date:    21:43:48 12/22/2017
-- Design Name:
-- Module Name:    SevenSegment - Behavioral
-- Project Name:
-- Target Devices:
-- Tool versions:
-- Description:
--
-- Dependencies:
--
-- Revision:
-- Revision 0.01 - File Created
-- Additional Comments:
--
----------------------------------------------------------------------------------
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

use ieee.std_logic_unsigned.all;
use ieee.numeric_std.all;
-- Uncomment the following library declaration if using
-- arithmetic functions with Signed or Unsigned values
--use IEEE.NUMERIC_STD.ALL;

-- Uncomment the following library declaration if instantiating
-- any Xilinx primitives in this code.
--library UNISIM;
--use UNISIM.VComponents.all;

entity SevenSegment is
	port (
		clk				: in std_logic;		-- main clock signal
		reset			: in std_logic;		-- high active signal (reset when reset = '1')

		L1				: in std_logic;
		Dis1			: in std_logic_vector(7 downto 0);
		L2				: in std_logic;
		Dis2			: in std_logic_vector(7 downto 0);

		SegDis		: out std_logic_vector(10 downto 0)
	);
end SevenSegment;

architecture Behavioral of SevenSegment is
		type Segs is (
			Seg10,
			Seg11,
			Seg20,
			Seg21,
			Seg30,
			Seg31,
			Seg40,
			Seg41
		);

		signal State, State_next : Segs;

		signal segments 	: std_logic_vector(6 downto 0);
		signal dis_sel 		: std_logic_vector(3 downto 0);
		signal cur_n	 	: std_logic_vector(3 downto 0);
		signal dis1_l		: std_logic_vector(7 downto 0);
		signal dis2_l		: std_logic_vector(7 downto 0);
begin

	SegDis <= dis_sel & segments;

	segState : process(reset, clk) begin
		if(reset = '1') then
			State_next <= Seg10;
			segments <= (others => '0');
			dis_sel <= (others => '0');
			dis1_l <= (others => '0');
			dis2_l <= (others => '0');
			cur_n <= (others => '0');
		elsif rising_edge(clk) then
			if (L1='1') then
				dis1_l <= Dis1;
			end if;

			if (L2='1') then
				dis2_l <= Dis2;
			end if;

			case cur_n is
				when "0000" =>
					segments <= "1111101";
				when "0001" =>
					segments <= "1010000";
				when "0010" =>
					segments <= "0110111";
				when "0011" =>
					segments <= "1110110";
				when "0100" =>
					segments <= "1011010";
				when "0101" =>
					segments <= "1101110";
				when "0110" =>
					segments <= "1101111";
				when "0111" =>
					segments <= "1110000";
				when "1000" =>
					segments <= "1111111";
				when "1001" =>
					segments <= "1111010";
				when "1010" =>
					segments <= "1111011";
				when "1011" =>
					segments <= "1001111";
				when "1100" =>
					segments <= "0101101";
				when "1101" =>
					segments <= "1010111";
				when "1110" =>
					segments <= "0101111";
				when "1111" =>
					segments <= "0101011";
				when others => null;
			end case;

			case State is
				when Seg10 =>
					cur_n <= dis2_l(7 downto 4);
					dis_sel <= "0000";
					State_next <= Seg11;
				when Seg11 =>
					dis_sel <= "0001";
					State_next <= Seg20;
				when Seg20 =>
					cur_n <= dis2_l(3 downto 0);
					dis_sel <= "0000";
					State_next <= Seg21;
				when Seg21 =>
					dis_sel <= "0010";
					State_next <= Seg30;
				when Seg30 =>
					cur_n <= dis1_l(7 downto 4);
					dis_sel <= "0000";
					State_next <= Seg31;
				when Seg31 =>
					dis_sel <= "0100";
					State_next <= Seg40;
				when Seg40 =>
					cur_n <= dis1_l(3 downto 0);
					dis_sel <= "0000";
					State_next <= Seg41;
				when Seg41 =>
					dis_sel <= "1000";
					State_next <= Seg10;
				when others => null;
			end case;
		end if;
	end process;

	State <= State_next;


end Behavioral;
