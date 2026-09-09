"""BhashaSetu - AI-Powered Vernacular Pedagogy Tool"""

__version__ = "0.1.0"
__author__ = "Team Quixotic"

from .src.models import NMTModel
from .src.inference import TranslationPipeline

__all__ = ["NMTModel", "TranslationPipeline"]
