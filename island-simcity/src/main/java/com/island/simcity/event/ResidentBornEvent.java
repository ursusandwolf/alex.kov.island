package com.island.simcity.event;

import com.island.simcity.entities.SimEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ResidentBornEvent {
    private final SimEntity resident;
}
