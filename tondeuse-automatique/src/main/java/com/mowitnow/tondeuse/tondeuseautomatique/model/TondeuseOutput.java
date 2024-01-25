package com.mowitnow.tondeuse.tondeuseautomatique.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TondeuseOutput {
    private int xm;
    private int ym;
    private char orientationm;
}
