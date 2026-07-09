# Mock of xgboost library in pure Python to bypass binary compilation requirements
import numpy as np

class DMatrix:
    def __init__(self, data, label=None, **kwargs):
        pass

class XGBRegressor:
    def __init__(self, **kwargs):
        self.feature_importances_ = np.array([])
    def fit(self, *args, **kwargs):
        pass
    def predict(self, data, *args, **kwargs):
        if hasattr(data, 'shape'):
            return np.zeros(data.shape[0])
        elif hasattr(data, '__len__'):
            return np.zeros(len(data))
        return np.zeros(1)
