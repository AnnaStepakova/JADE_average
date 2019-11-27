#!/usr/bin/env python3

import numpy as np

A = np.matrix([[0, 0, 0, 0, 1], 
               [1, 0, 0, 0, 0], 
               [0, 1, 0, 0, 0], 
               [0, 1, 1, 0, 0], 
               [0, 1, 0, 1, 0]])
               
D = np.diagflat(np.sum(A, 1))
L = D - A
Z = np.linalg.eigvals(L)

X = np.array([13, 21, 35, -5, 82])
print("Weighted average: {}".format(np.sum(np.dot(Z, X)) / np.sum(Z)))
print("Weighted average abs: {}".format(np.absolute(np.sum(np.dot(Z, X)) / np.sum(Z))))
print("Mean: {}".format(np.sum(X) / np.size(X)))
