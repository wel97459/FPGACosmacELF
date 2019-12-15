library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

entity fpga_rom is
    port (
        clk: in std_logic;
        addr: in std_logic_vector(4 downto 0);
        data: out std_logic_vector(7 downto 0)
    );
end entity;

architecture Behavioral of fpga_rom is

    type mtype is array(0 to 10) of std_logic_vector(7 downto 0);

    constant rom_data: mtype := (
        x"E1",
        x"F8",
        x"00",
        x"B1",
        x"F8",
        x"0A",
        x"A1",
        x"13",
        x"30",
        x"07",
        x"A5"
    );
    signal add_int: integer range 0 to 10;
begin
    add_int <= to_integer(unsigned(addr));
    process(clk) begin
        if rising_edge(clk) then
            if add_int <= 10 then
                data <= rom_data(add_int);
            else
                data <= (others => '0');
            end if;
        end if;
    end process;
end Behavioral;