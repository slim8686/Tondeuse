package com.mowitnow.tondeuse.tondeuseautomatique.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TondeuseInput {
    private int x0;
    private int y0;
    private char orientation0;
    private String commande;


}
