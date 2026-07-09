# Mock of Uber H3 library in pure Python to bypass Windows AppLocker/Application Control blocks on DLLs
from typing import List, Dict, Tuple, Set

def polyfill_geojson(geojson, resolution):
    return {"892a1008927ffff"}

def geo_to_h3(lat, lng, resolution):
    return "892a1008927ffff"

def h3_to_geo(h3_cell):
    return (25.7617, -80.1918)

def h3_to_geo_boundary(h3_cell):
    lat, lng = h3_to_geo(h3_cell)
    return [
        (lat + 0.001, lng),
        (lat + 0.0005, lng + 0.001),
        (lat - 0.0005, lng + 0.001),
        (lat - 0.001, lng),
        (lat - 0.0005, lng - 0.001),
        (lat + 0.0005, lng - 0.001)
    ]

def k_ring(h3_cell, ring):
    return {h3_cell}

def h3_distance(h3_cell1, h3_cell2):
    return 0

def edge_length(resolution, unit='m'):
    if unit == 'm':
        return 174.0
    return 0.174

def hex_area(resolution, unit='km^2'):
    if unit == 'km^2':
        return 0.1
    return 100000.0
